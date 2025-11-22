package kwonjh0406.joondrive.file.dto;

public class MoveRequest {
    private Long fileId;
    private Long newParentId;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getNewParentId() {
        return newParentId;
    }

    public void setNewParentId(Long newParentId) {
        this.newParentId = newParentId;
    }
}

