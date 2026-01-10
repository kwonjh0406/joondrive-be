package kwonjh0406.joondrive.file.dto;

import kwonjh0406.joondrive.file.entity.FileEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileResponse {
    private Long id;
    private Long parentId;
    private String fileType;
    private String name;
    private Long size;
    private LocalDateTime createdAt;
    
    public static FileResponse from(FileEntity entity) {
        return FileResponse.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .fileType(entity.getFileType())
                .name(entity.getName())
                .size(entity.getSize())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
