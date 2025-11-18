package kwonjh0406.joondrive.drive.service;

import kwonjh0406.joondrive.auth.entity.User;
import kwonjh0406.joondrive.drive.dto.DriveInfoResponse;
import kwonjh0406.joondrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class DriveService {

    @Value("${storage.base-path}")
    private String BASE_STORAGE_PATH;

    private final UserRepository userRepository;

    public DriveInfoResponse getDriveInfo(Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 사용자 폴더 경로: /joondrive/{userId}
        Path userFolderPath = Paths.get(BASE_STORAGE_PATH).resolve(userId.toString());
        
        // 폴더가 존재하지 않으면 사용량은 0
        long usedStorage = 0;
        if (Files.exists(userFolderPath)) {
            usedStorage = calculateFolderSize(userFolderPath);
        }

        return new DriveInfoResponse(
                user.getEmail(),
                usedStorage,
                user.getStorageLimit()
        );
    }

    /**
     * 사용자의 현재 스토리지 사용량을 조회합니다.
     * @param userId 사용자 ID
     * @return 현재 사용량 (바이트)
     */
    public long getUsedStorage(Long userId) throws IOException {
        Path userFolderPath = Paths.get(BASE_STORAGE_PATH).resolve(userId.toString());
        
        if (!Files.exists(userFolderPath)) {
            return 0;
        }
        
        return calculateFolderSize(userFolderPath);
    }

    /**
     * 사용자의 스토리지 한도를 조회합니다.
     * @param userId 사용자 ID
     * @return 스토리지 한도 (바이트, GB 단위를 바이트로 변환)
     */
    public long getStorageLimit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // GB를 바이트로 변환
        return (long) user.getStorageLimit() * 1024L * 1024L * 1024L;
    }

    /**
     * 리눅스 du 명령어를 사용하여 폴더의 총 크기를 한 번에 계산합니다.
     * @param folderPath 계산할 폴더 경로
     * @return 폴더의 총 크기 (바이트)
     */
    private long calculateFolderSize(Path folderPath) throws IOException {
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            return 0;
        }

        try {
            // du -sb: 바이트 단위로 총 크기만 출력
            ProcessBuilder processBuilder = new ProcessBuilder("du", "-sb", folderPath.toString());
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    // 출력 형식: "크기    경로"
                    String[] parts = line.split("\\s+");
                    if (parts.length > 0) {
                        return Long.parseLong(parts[0]);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("du 명령어 실행 실패: exit code " + exitCode);
            }
            
            return 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("du 명령어 실행 중 인터럽트 발생", e);
        } catch (NumberFormatException e) {
            throw new IOException("du 명령어 출력 파싱 실패", e);
        }
    }
}

