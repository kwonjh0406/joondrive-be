package kwonjh0406.joondrive.auth.controller;

import kwonjh0406.joondrive.auth.dto.EmailVerificationRequest;
import kwonjh0406.joondrive.auth.dto.EmailVerificationResponse;
import kwonjh0406.joondrive.auth.dto.SignupRequest;
import kwonjh0406.joondrive.auth.entity.User;
import kwonjh0406.joondrive.auth.security.CustomUserDetails;
import kwonjh0406.joondrive.auth.service.AuthService;
import kwonjh0406.joondrive.auth.service.EmailVerificationService;
import kwonjh0406.joondrive.common.annotation.CurrentUser;
import kwonjh0406.joondrive.common.dto.ApiResponse;
import kwonjh0406.joondrive.common.exception.CustomException;
import kwonjh0406.joondrive.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailService;

    @PostMapping("/verification-codes")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> sendCode(
            @RequestBody EmailVerificationRequest request) {
        int expiresIn = emailService.sendCode(request.getEmail());
        EmailVerificationResponse response = new EmailVerificationResponse(expiresIn);
        return ResponseEntity.ok(ApiResponse.ok(response, "인증번호가 발송되었습니다."));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다."));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verify(@CurrentUser CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        User user = userDetails.getUser();

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "authenticated", true,
                "email", user.getEmail(),
                "storageLimit", user.getStorageLimit(),
                "usedStorage", 0 // TODO: Implement real storage usage check in FileService
        )));
    }
}
