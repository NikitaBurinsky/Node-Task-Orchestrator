package nto.application.interfaces.services;

import nto.application.dto.BulkTaskRequestDto;
import nto.application.dto.TaskDto;
import nto.core.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {

    /**
     * Создает новую задачу (регистрирует запуск скрипта).
     *
     * @param dto DTO с serverId и scriptId
     * @return Созданная задача
     */
    TaskDto createTask(TaskDto dto);

    Page<TaskDto> getTasksWithFilters(String username, TaskStatus status, Pageable pageable);

    /**
     * Получает последний статус выполнения скрипта на сервере.
     * Использует In-Memory Cache для скорости.
     *
     * @param serverId ID сервера
     * @param scriptId ID скрипта
     * @return Последняя задача или null, если не найдена
     */
    TaskDto getLastStatus(Long serverId, Long scriptId);

    List<TaskDto> createTasksBulk(BulkTaskRequestDto dto);

    // Новые методы для просмотра результатов
    TaskDto getTaskById(Long id);

    List<TaskDto> getAllTasks();
}