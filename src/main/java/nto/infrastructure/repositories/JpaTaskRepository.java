package nto.infrastructure.repositories;

import nto.application.interfaces.repositories.TaskRepository;
import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaTaskRepository extends JpaRepository<TaskEntity, Long>, TaskRepository {

    Optional<TaskEntity> findFirstByServerIdAndScriptIdOrderByCreatedAtDesc(Long serverId,
                                                                            Long scriptId);

    @Query("SELECT DISTINCT t FROM TaskEntity t JOIN t.server s JOIN s.groups g " +
        "WHERE g.owner.username = :username")
    List<TaskEntity> findAllByServerGroupOwnerUsername(@Param("username") String username);

    @Query("SELECT t FROM TaskEntity t WHERE t.sourceGroup.id = :groupId " +
        "AND t.createdAt = (SELECT MAX(t2.createdAt) FROM TaskEntity t2 WHERE t2.sourceGroup.id = :groupId)")
    List<TaskEntity> findLatestTasksByGroupId(@Param("groupId") Long groupId);

    boolean existsByServerIdAndStatusIn(Long serverId, Collection<TaskStatus> statuses);
    
    @Query("SELECT DISTINCT t FROM TaskEntity t " +
        "JOIN t.server s JOIN s.groups g " +
        "WHERE g.owner.username = :username " +
        "AND (:status IS NULL OR t.status = :status)")
    Page<TaskEntity> findTasksByUserAndStatusJPQL(
        @Param("username") String username,
        @Param("status") TaskStatus status,
        Pageable pageable
    );
    
    @Query(
        value = "SELECT DISTINCT t.* FROM tasks t " +
            "JOIN servers s ON t.server_id = s.id " +
            "JOIN server_groups sg ON sg.server_id = s.id " +
            "JOIN groups g ON sg.group_id = g.id " +
            "JOIN users u ON g.user_id = u.id " +
            "WHERE u.username = :username " +
            "AND (CAST(:#{#status != null ? #status.name() : null} AS task_status) IS NULL " +
            "  OR t.status = CAST(:#{#status != null ? #status.name() : null} AS task_status))",
        countQuery = "SELECT COUNT(DISTINCT t.id) FROM tasks t " +
            "JOIN servers s ON t.server_id = s.id " +
            "JOIN server_groups sg ON sg.server_id = s.id " +
            "JOIN groups g ON sg.group_id = g.id " +
            "JOIN users u ON g.user_id = u.id " +
            "WHERE u.username = :username " +
            "AND (CAST(:#{#status != null ? #status.name() : null} AS task_status) IS NULL " +
            "  OR t.status = CAST(:#{#status != null ? #status.name() : null} AS task_status))",
        nativeQuery = true
    )
    Page<TaskEntity> findTasksByUserAndStatusNative(
        @Param("username") String username,
        @Param("status") TaskStatus status,
        Pageable pageable
    );
}
