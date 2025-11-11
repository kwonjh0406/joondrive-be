package kwonjh0406.joondrive.controller;

import jakarta.servlet.http.HttpSession;
import kwonjh0406.joondrive.domain.User;
import kwonjh0406.joondrive.dto.SendCodeRequest;
import kwonjh0406.joondrive.dto.SignupRequest;
import kwonjh0406.joondrive.repository.UserRepository;
import kwonjh0406.joondrive.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;

    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestBody SendCodeRequest request) {
        emailService.sendCode(request.getEmail());
        return ResponseEntity.ok("Code sent");
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
        user.setVerified(true);
        userRepository.save(user);

        session.setAttribute("user", email); // 로그인 세션
        return ResponseEntity.ok("Signup success");
    }
}
