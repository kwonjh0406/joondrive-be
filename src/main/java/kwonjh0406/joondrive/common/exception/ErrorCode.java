package kwonjh0406.joondrive.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "접근 권한이 없습니다."),

    // File
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."),
    INVALID_FILE_PATH(HttpStatus.BAD_REQUEST, "유효하지 않은 파일 경로입니다."),
    STORAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "스토리지 한도를 초과했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 다운로드에 실패했습니다."),
    FILE_DOES_NOT_EXIST(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다."),
    FOLDER_DOWNLOAD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "폴더는 다운로드할 수 없습니다."),
    NO_FILES_SELECTED(HttpStatus.BAD_REQUEST, "다운로드할 파일을 선택해주세요."),
    ZIP_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "압축 파일 생성 중 오류가 발생했습니다."),
    INVALID_MOVE_TARGET(HttpStatus.BAD_REQUEST, "유효하지 않은 이동 대상입니다."),
    CIRCULAR_REFERENCE_DETECTED(HttpStatus.BAD_REQUEST, "순환 참조가 감지되었습니다."),

    // Global
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력값입니다.");

    private final HttpStatus status;
    private final String message;
}
