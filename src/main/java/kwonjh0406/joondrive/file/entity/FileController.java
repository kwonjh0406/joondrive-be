package kwonjh0406.joondrive.file.entity;

import jakarta.servlet.http.HttpServletRequest;
import kwonjh0406.joondrive.auth.entity.User;
import kwonjh0406.joondrive.drive.service.DriveService;
import kwonjh0406.joondrive.file.exception.StorageLimitExceededException;
import kwonjh0406.joondrive.file.repository.FileRepository;
import kwonjh0406.joondrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
class FileController {

    private final FileRepository fileRepository;

    @Value("${storage.base-path}")
    private String BASE_STORAGE_PATH;
    private final UserRepository userRepository;
    private final DriveService driveService;

    private Path getUserBasePath(Long userId) throws IOException {
        Path base = Paths.get(BASE_STORAGE_PATH).resolve(userId.toString());
        if (!Files.exists(base)) Files.createDirectories(base);
        return base;
    }

    // 파일 조회
    @GetMapping
    public ResponseEntity<List<FileEntity>> listFiles(@RequestParam(required = false) Long parentId, HttpServletRequest req) {
        Long userId = getUserId();
        List<FileEntity> files = fileRepository.findByUserIdAndParentId(userId, parentId);
        return ResponseEntity.ok(files);
    }

    // 파일 업로드
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFiles(@RequestParam("files") List<MultipartFile> files,
                                              @RequestParam(required = false) Long parentId,
                                              HttpServletRequest req) throws IOException {
        Long userId = getUserId();
        
        // 업로드할 파일들의 총 크기 계산
        long totalUploadSize = files.stream()
                .filter(file -> !file.isEmpty())
                .mapToLong(MultipartFile::getSize)
                .sum();
        
        // 현재 사용량과 한도 확인
        long currentUsedStorage = driveService.getUsedStorage(userId);
        long storageLimit = driveService.getStorageLimit(userId);
        
        // 업로드 후 예상 사용량이 한도를 초과하는지 검증
        if (currentUsedStorage + totalUploadSize > storageLimit) {
            long availableSpace = storageLimit - currentUsedStorage;
            throw new StorageLimitExceededException(
                    String.format("스토리지 한도를 초과합니다. 사용 가능한 용량: %d 바이트 (%.2f MB)", 
                            availableSpace, availableSpace / (1024.0 * 1024.0))
            );
        }
        
        Path userBase = getUserBasePath(userId);

        for (MultipartFile multipartFile : files) {
            if (multipartFile.isEmpty()) continue;

            String originalName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
            String ext = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex > 0) ext = originalName.substring(dotIndex);

            String uniqueName = UUID.randomUUID() + ext;
            Path filePath = userBase.resolve(uniqueName);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(multipartFile.getBytes());
            }

            FileEntity file = new FileEntity();
            file.setUserId(userId);
            file.setParentId(parentId);
            file.setFileType("file");
            file.setName(originalName);
            file.setLogicalPath(originalName);
            file.setRealPath(filePath.toString());
            file.setSize(multipartFile.getSize());

            fileRepository.save(file);
        }

        return ResponseEntity.ok("업로드 완료");
    }

    // 파일 삭제
    @PostMapping("/delete")
    public ResponseEntity<String> deleteFiles(@RequestBody List<Long> ids, HttpServletRequest req) throws IOException {
        Long userId = getUserId();
        List<FileEntity> files = fileRepository.findAllById(ids);

        for (FileEntity file : files) {
            if (!file.getUserId().equals(userId)) continue;
            if ("file".equals(file.getFileType()) && file.getRealPath() != null) {
                Path path = Paths.get(file.getRealPath());
                Files.deleteIfExists(path);
            }
            fileRepository.delete(file);
        }
        return ResponseEntity.ok("삭제 완료");
    }

    // 파일 다운로드
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId, HttpServletRequest req) throws IOException {
        Long userId = getUserId();
        
        // 파일 조회
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        
        // 권한 확인: 본인 파일인지 확인
        if (!file.getUserId().equals(userId)) {
            throw new RuntimeException("파일 다운로드 권한이 없습니다.");
        }
        
        // 파일 타입 확인: 폴더는 다운로드 불가
        if (!"file".equals(file.getFileType())) {
            throw new RuntimeException("폴더는 다운로드할 수 없습니다.");
        }
        
        // 실제 파일 경로 확인
        if (file.getRealPath() == null || file.getRealPath().isEmpty()) {
            throw new RuntimeException("파일 경로가 없습니다.");
        }
        
        Path filePath = Paths.get(file.getRealPath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("파일이 존재하지 않습니다.");
        }
        
        // 파일을 Resource로 변환
        Resource resource = new FileSystemResource(filePath);
        
        // 파일명 인코딩 (한글 파일명 지원)
        String fileName = file.getName();
        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        
        // 응답 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(Files.size(filePath))
                .body(resource);
    }

    // 폴더 생성
    @PostMapping("/folders")
    public ResponseEntity<FileEntity> createFolder(@RequestBody FolderRequest folderRequest, HttpServletRequest req) {
        Long userId = getUserId();

        FileEntity folder = new FileEntity();
        folder.setUserId(userId);
        folder.setParentId(folderRequest.getParentId());
        folder.setFileType("folder");
        folder.setName(folderRequest.getName());
        folder.setLogicalPath(folderRequest.getName());
        folder.setSize(0L);
        folder.setRealPath(null);

        fileRepository.save(folder);
        return ResponseEntity.ok(folder);
    }

    public Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByEmail(authentication.getName()).get();


        return user.getId(); // 인증되지 않은 경우
    }

    static class FolderRequest {
        private Long parentId;
        private String name;

        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}