package kwonjh0406.joondrive.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DriveInfoResponse {
    private String email;
    private Long usedStorage; // 바이트 단위
    private Integer storageLimit; // GB 단위
}

