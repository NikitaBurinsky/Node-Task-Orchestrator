package nto.application.interfaces.repositories;

import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskRepository {
    List<TaskEntity> findLatestTasksByGroupId(Long groupId);

    Page<TaskEntity> findTasksByUserAndStatusJPQL(String username, TaskStatus status,
                                                  Pageable pageable);

    Page<TaskEntity> findTasksByUserAndStatusNative(String username, TaskStatus status,
                                                    Pageable pageable);
}