package kwonjh0406.joondrive.file.entity;

import jakarta.servlet.http.HttpServletRequest;
import kwonjh0406.joondrive.domain.User;
import kwonjh0406.joondrive.file.repository.FileRepository;
import kwonjh0406.joondrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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