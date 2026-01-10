package kwonjh0406.joondrive.file.repository;

import kwonjh0406.joondrive.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUserIdAndParentId(Long userId, Long parentId);
    
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileEntity f WHERE f.userId = :userId")
    Long calculateUsedStorage(@Param("userId") Long userId);
}
