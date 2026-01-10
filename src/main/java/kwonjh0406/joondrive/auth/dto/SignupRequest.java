package kwonjh0406.joondrive.auth.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String code;
}
