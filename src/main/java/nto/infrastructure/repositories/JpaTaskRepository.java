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

    List<TaskEntity> findAllByServerOwnerUsername(String username);

    @Query("SELECT t FROM TaskEntity t WHERE t.sourceGroup.id = :groupId " +
        "AND t.createdAt = (SELECT MAX(t2.createdAt) FROM TaskEntity t2 WHERE t2.sourceGroup.id = :groupId)")
    List<TaskEntity> findLatestTasksByGroupId(@Param("groupId") Long groupId);

    boolean existsByServerIdAndStatusIn(Long serverId, Collection<TaskStatus> statuses);

    //
     // ДЕМОНСТРАЦИЯ ДЛЯ 3 ЛАБЫ

    // 1. JPQL: Фильтрация по вложенным сущностям (Server -> Owner)
    @Query("SELECT t FROM TaskEntity t " +
        "WHERE t.server.owner.username = :username " +
        "AND (:status IS NULL OR t.status = :status)")
    Page<TaskEntity> findTasksByUserAndStatusJPQL(
        @Param("username") String username,
        @Param("status") TaskStatus status,
        Pageable pageable
    );

    //TODO
    // Native SQL
    // 1. Прописываем явные JOIN через внешние ключи
    // 2. Добавляем countQuery для корректной работы Page<T> (аналог .Count() в LINQ перед .Skip().Take())
    // 3. Используем CAST(... AS text), чтобы Postgres не ругался на неизвестный тип при передаче null
    @Query(
        value = "SELECT t.* FROM tasks t " +
            "JOIN servers s ON t.server_id = s.id " +
            "JOIN users u ON s.user_id = u.id " +
            "WHERE u.username = :username " +
            "AND (CAST(:#{#status != null ? #status.name() : null} AS text) IS NULL " +
            "  OR t.status = CAST(:#{#status != null ? #status.name() : null} AS text))",
        countQuery = "SELECT COUNT(t.id) FROM tasks t " +
            "JOIN servers s ON t.server_id = s.id " +
            "JOIN users u ON s.user_id = u.id " +
            "WHERE u.username = :username " +
            "AND (CAST(:#{#status != null ? #status.name() : null} AS text) IS NULL " +
            "  OR t.status = CAST(:#{#status != null ? #status.name() : null} AS text))",
        nativeQuery = true
    )
    Page<TaskEntity> findTasksByUserAndStatusNative(
        @Param("username") String username,
        @Param("status") TaskStatus status,
        Pageable pageable
    );
}