package kwonjh0406.joondrive.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationResponse {
    private int expiresIn;
}
