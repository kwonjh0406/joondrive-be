package kwonjh0406.joondrive.auth.service;

import kwonjh0406.joondrive.auth.entity.VerificationCode;
import kwonjh0406.joondrive.auth.repository.UserRepository;
import kwonjh0406.joondrive.auth.repository.VerificationCodeRepository;
import kwonjh0406.joondrive.common.exception.CustomException;
import kwonjh0406.joondrive.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;

    private static final int EXPIRE_SECONDS = 300;

    @Transactional
    public int sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            // throw new EmailAlreadyExistsException("이미 가입된 이메일입니다.");
            // Using CustomException standardized
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String code = generateCode();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(EXPIRE_SECONDS);

        VerificationCode verificationCode = verificationCodeRepository.findById(email)
                .orElse(new VerificationCode(email, code, expiryDate));

        verificationCode.updateCode(code, expiryDate);
        verificationCodeRepository.save(verificationCode);

        sendEmail(email, code);

        return EXPIRE_SECONDS;
    }

    @Transactional
    public boolean verifyCode(String email, String code) {
        VerificationCode verificationCode = verificationCodeRepository.findById(email).orElse(null);

        if (verificationCode == null)
            return false;

        if (verificationCode.isExpired()) {
            verificationCodeRepository.delete(verificationCode);
            return false;
        }

        boolean isValid = verificationCode.getCode().equals(code);
        if (isValid) {
            verificationCodeRepository.delete(verificationCode);
        }
        return isValid;
    }

    private String generateCode() {
        return String.format("%06d", (int) (Math.random() * 1_000_000));
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Joon Drive - 이메일 인증");
        message.setText("인증번호: " + code + "\n\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }
}
