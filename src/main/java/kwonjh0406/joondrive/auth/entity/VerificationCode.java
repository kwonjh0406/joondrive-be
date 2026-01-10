package kwonjh0406.joondrive.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VerificationCode {

    @Id
    private String email;

    private String code;

    private LocalDateTime expiryDate;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    public void updateCode(String code, LocalDateTime expiryDate) {
        this.code = code;
        this.expiryDate = expiryDate;
    }
}
