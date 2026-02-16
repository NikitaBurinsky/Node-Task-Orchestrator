package nto.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nto.application.dto.BulkTaskRequestDto;
import nto.application.dto.TaskDto;
import nto.application.interfaces.services.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Управление запуском скриптов")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Запустить скрипт", description = "Создает задачу на выполнение скрипта на сервере")
    public ResponseEntity<TaskDto> createTask(@RequestBody TaskDto dto) {
        // Валидацию добавим следующим шагом
        return ResponseEntity.ok(taskService.createTask(dto));
    }

    @GetMapping("/status")
    @Operation(summary = "Получить статус из кэша", description = "Быстрый опрос статуса (Hot Data) без лишних запросов в БД")
    public ResponseEntity<TaskDto> getTaskStatus(
            @RequestParam Long serverId,
            @RequestParam Long scriptId) {

        TaskDto task = taskService.getLastStatus(serverId, scriptId);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(task);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Массовый запуск", description = "Транзакционный запуск скрипта на группе серверов")
    public ResponseEntity<List<TaskDto>> createBulk(@RequestBody @Valid BulkTaskRequestDto dto) {
        return ResponseEntity.ok(taskService.createTasksBulk(dto));
    }
}