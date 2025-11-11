package kwonjh0406.joondrive.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private Map<String, String> codeMap = new ConcurrentHashMap<>();

    public void sendCode(String email) {
        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        codeMap.put(email, code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("회원가입 인증 코드");
        message.setText("인증 코드: " + code);
        mailSender.send(message);
    }

    public boolean verifyCode(String email, String code) {
        return codeMap.getOrDefault(email, "").equals(code);
    }
}
