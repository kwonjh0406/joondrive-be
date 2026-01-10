package kwonjh0406.joondrive.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }
    
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }
    
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> fail(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
    
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
