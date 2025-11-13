package kwonjh0406.joondrive.auth.service;

import kwonjh0406.joondrive.auth.exception.EmailAlreadyExistsException;
import kwonjh0406.joondrive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final Map<String, CodeInfo> codeMap = new ConcurrentHashMap<>();

    private static final int EXPIRE_SECONDS = 300;

    public int sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("이미 가입된 이메일입니다.");
        }

        String code = generateCode();
        codeMap.put(email, new CodeInfo(code, Instant.now()));
        sendEmail(email, code);

        return EXPIRE_SECONDS;
    }

    public boolean verifyCode(String email, String code) {
        CodeInfo codeInfo = codeMap.get(email);
        if (codeInfo == null) return false;

        if (isExpired(codeInfo)) {
            codeMap.remove(email);
            return false;
        }

        boolean isValid = codeInfo.code().equals(code);
        if (isValid) codeMap.remove(email);
        return isValid;
    }

    private String generateCode() {
        return String.format("%06d", (int) (Math.random() * 1_000_000));
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[" + code + "] Joon Drive");
        message.setText("다음 인증번호를 입력하여 회원가입을 마무리하세요.\n\n" +
                "인증번호: " + code);
        mailSender.send(message);
    }

    private boolean isExpired(CodeInfo codeInfo) {
        return java.time.Duration.between(codeInfo.createdAt(), Instant.now()).getSeconds() >= EXPIRE_SECONDS;
    }

    private record CodeInfo(String code, Instant createdAt) {
    }
}
