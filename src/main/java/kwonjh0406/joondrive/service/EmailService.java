package kwonjh0406.joondrive.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // 이메일 -> 코드 + 발급시간
    private final Map<String, CodeInfo> codeMap = new ConcurrentHashMap<>();

    private static final int EXPIRE_SECONDS = 300; // 5분 = 300초
    private final Random random = new Random();

    /**
     * 인증코드 발급 후 유효시간(초) 반환
     */
    public int sendCode(String email) {
        String code = String.format("%06d", random.nextInt(900000) + 100000);
        CodeInfo codeInfo = new CodeInfo(code, Instant.now());
        codeMap.put(email, codeInfo);

        // 이메일 전송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("회원가입 인증 코드");
        message.setText("인증 코드: " + code + "\n유효 시간: " + EXPIRE_SECONDS + "초");
        mailSender.send(message);

        return EXPIRE_SECONDS;
    }

    /**
     * 인증코드 검증
     */
    public boolean verifyCode(String email, String code) {
        CodeInfo codeInfo = codeMap.get(email);
        if (codeInfo == null) return false;

        long elapsedSeconds = java.time.Duration.between(codeInfo.getCreatedAt(), Instant.now()).getSeconds();
        if (elapsedSeconds >= EXPIRE_SECONDS) {
            codeMap.remove(email); // 만료 시 제거
            return false;
        }

        boolean isValid = codeInfo.getCode().equals(code);
        if (isValid) codeMap.remove(email); // 검증 성공 시 제거
        return isValid;
    }

    // 코드 + 발급 시간 DTO
    private static class CodeInfo {
        private final String code;
        private final Instant createdAt;

        public CodeInfo(String code, Instant createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }

        public String getCode() { return code; }
        public Instant getCreatedAt() { return createdAt; }
    }
}
