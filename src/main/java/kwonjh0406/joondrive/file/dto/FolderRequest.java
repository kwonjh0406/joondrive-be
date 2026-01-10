package kwonjh0406.joondrive.file.dto;

import lombok.Data;

@Data
public class FolderRequest {
    private Long parentId;
    private String name;
}
