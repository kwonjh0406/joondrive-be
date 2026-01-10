package kwonjh0406.joondrive.drive.controller;

import kwonjh0406.joondrive.auth.security.CustomUserDetails;
import kwonjh0406.joondrive.common.annotation.CurrentUser;
import kwonjh0406.joondrive.common.dto.ApiResponse;
import kwonjh0406.joondrive.drive.dto.DriveInfoResponse;
import kwonjh0406.joondrive.drive.service.DriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drive")
public class DriveController {


    private final DriveService driveService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DriveInfoResponse>> getDriveInfo(@CurrentUser CustomUserDetails userDetails) throws IOException {
        DriveInfoResponse driveInfo = driveService.getDriveInfo(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(driveInfo, "드라이브 정보 조회 성공"));
    }
}
