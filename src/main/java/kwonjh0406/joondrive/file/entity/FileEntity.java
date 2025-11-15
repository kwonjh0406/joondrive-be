package kwonjh0406.joondrive.file.entity;

import jakarta.persistence.*;

@Entity
public class FileEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long parentId;

    private String fileType;

    private String logicalPath;

    private String name;

    private Long size;

    private String realPath;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getLogicalPath() { return logicalPath; }
    public void setLogicalPath(String logicalPath) { this.logicalPath = logicalPath; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getRealPath() { return realPath; }
    public void setRealPath(String realPath) { this.realPath = realPath; }
}
