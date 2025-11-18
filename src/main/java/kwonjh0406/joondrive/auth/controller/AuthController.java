package kwonjh0406.joondrive.auth.controller;

import jakarta.servlet.http.HttpSession;
import kwonjh0406.joondrive.auth.dto.EmailVerificationRequest;
import kwonjh0406.joondrive.auth.dto.EmailVerificationResponse;
import kwonjh0406.joondrive.auth.service.EmailVerificationService;
import kwonjh0406.joondrive.auth.entity.User;
import kwonjh0406.joondrive.dto.SignupRequest;
import kwonjh0406.joondrive.global.ApiResponse;
import kwonjh0406.joondrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailVerificationService emailService;

    @PostMapping("/verification-codes")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> sendCode(@RequestBody EmailVerificationRequest request) {
        int expiresIn = emailService.sendCode(request.getEmail());
        EmailVerificationResponse response = new EmailVerificationResponse(expiresIn);
        return ResponseEntity.ok(ApiResponse.ok(response, "인증번호가 발송되었습니다."));
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request, HttpSession session) {
        String email = request.getEmail();
        String password = request.getPassword();
        String code = request.getCode();

        if (!emailService.verifyCode(email, code)) {
            return ResponseEntity.status(400).body("Invalid code");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(400).body("Email exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        session.setAttribute("user", email); // 로그인 세션
        return ResponseEntity.ok("Signup success");
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // 세션이 없거나 인증 안된 경우
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false));
        }
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "email", authentication.getName(),
                "storageLimit", userRepository.findByEmail(authentication.getName()).get().getStorageLimit(),
                "usedStorage", 1
        ));
    }


}
