package nto.application.interfaces.services;

import nto.application.dto.BulkTaskRequestDto;
import nto.application.dto.TaskDto;
import nto.core.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {


    TaskDto createTask(TaskDto dto);

    Page<TaskDto> getTasksWithFilters(String username, TaskStatus status, Pageable pageable);


    TaskDto getLastStatus(Long serverId, Long scriptId);

    List<TaskDto> createTasksBulk(BulkTaskRequestDto dto);


    TaskDto getTaskById(Long id);

    List<TaskDto> getAllTasks();
}