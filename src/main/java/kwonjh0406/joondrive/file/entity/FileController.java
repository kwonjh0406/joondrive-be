package kwonjh0406.joondrive.file.entity;

import jakarta.servlet.http.HttpServletRequest;
import kwonjh0406.joondrive.auth.entity.User;
import kwonjh0406.joondrive.drive.service.DriveService;
import kwonjh0406.joondrive.file.dto.MoveRequest;
import kwonjh0406.joondrive.file.exception.StorageLimitExceededException;
import kwonjh0406.joondrive.file.repository.FileRepository;
import kwonjh0406.joondrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    // 다중 파일 ZIP 다운로드
    @PostMapping("/download/zip")
    public ResponseEntity<Resource> downloadFilesAsZip(@RequestBody List<Long> fileIds, HttpServletRequest req) {
        Long userId = getUserId();

        // 요청 검증: 빈 배열 체크
        if (fileIds == null || fileIds.isEmpty()) {
            throw new RuntimeException("다운로드할 파일을 선택해주세요.");
        }

        // 파일 조회 및 권한 확인
        List<FileEntity> filesToDownload = new ArrayList<>();
        for (Long fileId : fileIds) {
            FileEntity file = fileRepository.findById(fileId)
                    .orElse(null);
            
            if (file == null) {
                continue; // 존재하지 않는 파일은 무시
            }

            // 권한 확인: 본인 파일인지 확인
            if (!file.getUserId().equals(userId)) {
                throw new RuntimeException("다운로드할 권한이 없는 파일이 포함되어 있습니다.");
            }

            filesToDownload.add(file);
        }

        // 다운로드할 파일이 없는 경우
        if (filesToDownload.isEmpty()) {
            throw new RuntimeException("다운로드할 파일을 찾을 수 없습니다.");
        }

        try {
            // ZIP 파일 생성
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            
            // 파일명 충돌 방지를 위한 Set (이미 사용된 경로 추적)
            Set<String> usedPaths = new HashSet<>();

            for (FileEntity file : filesToDownload) {
                addToZip(zos, file, "", userId, usedPaths);
            }

            zos.close();
            byte[] zipBytes = baos.toByteArray();

            // 응답 생성
            ByteArrayResource resource = new ByteArrayResource(zipBytes);

            // 파일명 인코딩 (한글 파일명 지원)
            String fileName = "download.zip";
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);
            headers.setContentLength(zipBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            throw new RuntimeException("압축 파일 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ZIP에 파일/폴더를 재귀적으로 추가하는 메서드
    private void addToZip(ZipOutputStream zos, FileEntity file, String basePath, Long userId, Set<String> usedPaths) throws IOException {
        String entryName = basePath + file.getName();
        
        if ("folder".equals(file.getFileType())) {
            // 폴더인 경우: 먼저 "/" 추가 후 충돌 체크
            if (!entryName.endsWith("/")) {
                entryName += "/";
            }
            
            // 파일명 충돌 처리 (폴더 경로로)
            entryName = resolveNameConflict(entryName, usedPaths);
            usedPaths.add(entryName);
            
            // 빈 폴더도 ZIP에 포함 (일부 ZIP 뷰어에서 필요)
            ZipEntry folderEntry = new ZipEntry(entryName);
            zos.putNextEntry(folderEntry);
            zos.closeEntry();

            // 하위 파일들 조회 및 재귀적으로 추가
            List<FileEntity> children = fileRepository.findByUserIdAndParentId(userId, file.getId());
            for (FileEntity child : children) {
                addToZip(zos, child, entryName, userId, usedPaths);
            }
        } else {
            // 파일인 경우: 파일명 충돌 처리
            entryName = resolveNameConflict(entryName, usedPaths);
            usedPaths.add(entryName);
            
            // 실제 파일 내용 추가
            if (file.getRealPath() == null || file.getRealPath().isEmpty()) {
                return; // 경로가 없는 파일은 건너뛰기
            }

            Path filePath = Paths.get(file.getRealPath());
            if (!Files.exists(filePath)) {
                return; // 존재하지 않는 파일은 건너뛰기
            }

            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);

            // 파일 내용 읽기 및 ZIP에 쓰기
            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }

            zos.closeEntry();
        }
    }

    // 파일명 충돌 해결 메서드
    private String resolveNameConflict(String entryName, Set<String> usedPaths) {
        if (!usedPaths.contains(entryName)) {
            return entryName;
        }

        // 충돌이 발생하면 번호를 추가
        String baseName;
        String extension = "";
        boolean isFolder = entryName.endsWith("/");

        if (isFolder) {
            baseName = entryName.substring(0, entryName.length() - 1);
        } else {
            int lastDot = entryName.lastIndexOf('.');
            if (lastDot > 0) {
                baseName = entryName.substring(0, lastDot);
                extension = entryName.substring(lastDot);
            } else {
                baseName = entryName;
            }
        }

        int counter = 1;
        String newName;
        do {
            newName = baseName + "(" + counter + ")" + extension;
            if (isFolder) {
                newName += "/";
            }
            counter++;
        } while (usedPaths.contains(newName) && counter < 1000); // 무한 루프 방지

        return newName;
    }

    // 파일 이동
    @PutMapping("/move")
    public ResponseEntity<String> moveFile(@RequestBody MoveRequest moveRequest, HttpServletRequest req) {
        Long userId = getUserId();
        Long fileId = moveRequest.getFileId();
        Long newParentId = moveRequest.getNewParentId();

        // 이동할 파일 조회
        FileEntity fileToMove = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        // 사용자 소유권 확인
        if (!fileToMove.getUserId().equals(userId)) {
            throw new RuntimeException("파일 이동 권한이 없습니다.");
        }

        // newParentId가 null이 아닌 경우 검증
        if (newParentId != null) {
            // 새로운 부모 폴더 조회
            FileEntity newParent = fileRepository.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("대상 폴더를 찾을 수 없습니다."));

            // 사용자 소유권 확인
            if (!newParent.getUserId().equals(userId)) {
                throw new RuntimeException("대상 폴더에 대한 권한이 없습니다.");
            }

            // 폴더 타입 확인 (파일은 부모가 될 수 없음)
            if (!"folder".equals(newParent.getFileType())) {
                throw new RuntimeException("대상은 폴더여야 합니다.");
            }

            // 순환 참조 방지: 자기 자신을 부모로 설정하는 것 방지
            if (fileId.equals(newParentId)) {
                throw new RuntimeException("자기 자신을 부모 폴더로 설정할 수 없습니다.");
            }

            // 순환 참조 방지: newParentId가 fileId의 하위 폴더인지 확인 (폴더가 자기 자신의 하위로 이동하는 것 방지)
            if (isDescendantOf(newParentId, fileId, userId)) {
                throw new RuntimeException("하위 폴더를 부모 폴더로 설정할 수 없습니다.");
            }
        }

        // parentId 업데이트
        fileToMove.setParentId(newParentId);
        fileRepository.save(fileToMove);

        return ResponseEntity.ok("파일 이동 완료");
    }

    // 순환 참조 체크: descendantId가 ancestorId의 하위 폴더인지 확인하는 메서드
    // descendantId의 부모 체인을 따라가면서 ancestorId를 만나는지 확인
    private boolean isDescendantOf(Long descendantId, Long ancestorId, Long userId) {
        // descendantId가 폴더인지 확인 (폴더가 아니면 하위가 없으므로 체크 불필요)
        FileEntity descendant = fileRepository.findById(descendantId).orElse(null);
        if (descendant == null || !"folder".equals(descendant.getFileType())) {
            return false;
        }

        // 재귀적으로 부모 체인을 따라가면서 ancestorId를 만나는지 확인
        Long currentParentId = descendant.getParentId();
        int maxDepth = 100; // 무한 루프 방지
        int depth = 0;

        while (currentParentId != null && depth < maxDepth) {
            if (currentParentId.equals(ancestorId)) {
                return true; // descendant가 ancestor의 하위 폴더임
            }

            FileEntity parent = fileRepository.findById(currentParentId).orElse(null);
            if (parent == null || !parent.getUserId().equals(userId)) {
                break;
            }

            currentParentId = parent.getParentId();
            depth++;
        }

        return false;
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