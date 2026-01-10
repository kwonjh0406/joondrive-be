package kwonjh0406.joondrive.auth.service;

import kwonjh0406.joondrive.auth.dto.SignupRequest;
import kwonjh0406.joondrive.auth.entity.User;
import kwonjh0406.joondrive.auth.repository.UserRepository;
import kwonjh0406.joondrive.common.exception.CustomException;
import kwonjh0406.joondrive.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailService;

    @Transactional
    public void signup(SignupRequest request) {
        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. 인증 코드 검증
        if (!emailService.verifyCode(request.getEmail(), request.getCode())) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 3. 사용자 생성 및 저장
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStorageLimit(10); // 기본 10GB 제공
        
        userRepository.save(user);
    }
}
