package nto.application.interfaces.repositories;

import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;

import java.util.List;

public interface TaskRepository {
    // Методы для Лабы 3 (Сложные запросы)

    // 1. JPQL поиск
    List<TaskEntity> findByStatusAndServerSubnet(TaskStatus status, String subnet);

    // 2. Native SQL поиск
    List<TaskEntity> findByScriptIdNative(Long scriptId);
}