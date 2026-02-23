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

    // JPQL: Поиск по статусу и подсети сервера
    // В JPQL мы оперируем именами классов и полей, а не таблиц
    @Query("SELECT t FROM TaskEntity t WHERE t.status = :status AND t.server.ipAddress LIKE :subnet%")
    List<TaskEntity> findByStatusAndServerSubnet(
        @Param("status") TaskStatus status,
        @Param("subnet") String subnet
    );

    // Native SQL: Поиск по ID скрипта (для демонстрации)
    // Используем чистый SQL синтаксис Postgres
    @Query(value = "SELECT * FROM tasks WHERE script_id = :scriptId ORDER BY created_at DESC", nativeQuery = true)
    List<TaskEntity> findByScriptIdNative(@Param("scriptId") Long scriptId);

    Optional<TaskEntity> findFirstByServerIdAndScriptIdOrderByCreatedAtDesc(Long serverId,
                                                                            Long scriptId);

    // 2. Для getAllTasks: Найти все задачи, запущенные на серверах конкретного пользователя
    List<TaskEntity> findAllByServerOwnerUsername(String username);

    @Query("SELECT t FROM TaskEntity t WHERE t.sourceGroup.id = :groupId " +
        "AND t.createdAt = (SELECT MAX(t2.createdAt) FROM TaskEntity t2 WHERE t2.sourceGroup.id = :groupId)")
    List<TaskEntity> findLatestTasksByGroupId(@Param("groupId") Long groupId);

    boolean existsByServerIdAndStatusIn(Long serverId, Collection<TaskStatus> statuses);

    // 1. JPQL: Фильтрация по вложенным сущностям (Server -> Owner)
    @Query("SELECT t FROM TaskEntity t " +
        "WHERE t.server.owner.username = :username " +
        "AND (:status IS NULL OR t.status = :status)")
    Page<TaskEntity> findTasksByUserAndStatusJPQL(
        @Param("username") String username,
        @Param("status") TaskStatus status,
        Pageable pageable
    );
/*
    @Query(value = "SELECT t.* FROM tasks t " +
        "JOIN servers s ON t.server_id = s.id " +
        "JOIN users u ON s.owner_id = u.id " +
        "WHERE u.username = :username " +
        "AND (:status IS NULL OR t.status = :status)",
        countQuery = "SELECT count(*) FROM tasks t " +
            "JOIN servers s ON t.server_id = s.id " +
            "JOIN users u ON s.owner_id = u.id " +
            "WHERE u.username = :username",
        nativeQuery = true)
    Page<TaskEntity> findTasksByUserAndStatusNative(
        @Param("username") String username,
        @Param("status") String status,
        Pageable pageable
    );*/
}