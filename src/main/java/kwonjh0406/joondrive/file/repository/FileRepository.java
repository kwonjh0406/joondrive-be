package kwonjh0406.joondrive.file.repository;

import kwonjh0406.joondrive.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUserIdAndParentId(Long userId, Long parentId);
}
