package kwonjh0406.joondrive.drive.controller;

import kwonjh0406.joondrive.auth.entity.User;
import kwonjh0406.joondrive.drive.dto.DriveInfoResponse;
import kwonjh0406.joondrive.drive.service.DriveService;
import kwonjh0406.joondrive.global.ApiResponse;
import kwonjh0406.joondrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drive")
public class DriveController {

    private final DriveService driveService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DriveInfoResponse>> getDriveInfo() throws IOException {
        Long userId = getUserId();
        DriveInfoResponse driveInfo = driveService.getDriveInfo(userId);
        return ResponseEntity.ok(ApiResponse.ok(driveInfo, "드라이브 정보 조회 성공"));
    }

    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return user.getId();
    }
}
