// DTO 클래스 생성
package kwonjh0406.joondrive.dto;

public class SignupRequest {
    private String email;
    private String password;
    private String code;

    // getter, setter
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
}


