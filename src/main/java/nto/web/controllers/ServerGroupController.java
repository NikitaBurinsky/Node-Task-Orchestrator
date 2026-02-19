package nto.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerGroupDto;
import nto.application.dto.TaskDto;
import nto.application.interfaces.services.ServerGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Server Groups", description = "Управление группами серверов")
public class ServerGroupController {

    private final ServerGroupService groupService;

    @PostMapping
    @Operation(summary = "Создать группу", description = "Создает пустую группу серверов")
    public ResponseEntity<ServerGroupDto> create(@RequestBody @Valid ServerGroupDto dto) {
        return ResponseEntity.ok(groupService.createGroup(dto));
    }

    @GetMapping("/{id}/status/last")
    @Operation(summary = "Статус последнего запуска",
        description = "Возвращает список задач, созданных в рамках последнего массового запуска на этой группе")
    public ResponseEntity<List<TaskDto>> getLastGroupStatus(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getLastGroupExecutionStatus(id));
    }

    @GetMapping
    @Operation(summary = "Все группы", description = "Возвращает список групп текущего пользователя")
    public ResponseEntity<List<ServerGroupDto>> getAll() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerGroupDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getGroupById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить группу", description = "Удаляет группу, но НЕ удаляет серверы")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    // --- Управление составом ---

    @PostMapping("/{groupId}/servers/{serverId}")
    @Operation(summary = "Добавить сервер", description = "Добавляет существующий сервер в группу")
    public ResponseEntity<Void> addServer(@PathVariable Long groupId, @PathVariable Long serverId) {
        groupService.addServerToGroup(groupId, serverId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/servers/{serverId}")
    @Operation(summary = "Убрать сервер", description = "Исключает сервер из группы")
    public ResponseEntity<Void> removeServer(@PathVariable Long groupId,
                                             @PathVariable Long serverId) {
        groupService.removeServerFromGroup(groupId, serverId);
        return ResponseEntity.ok().build();
    }

    // --- Групповые операции ---

    @GetMapping("/{id}/ping")
    @Operation(summary = "Пинг группы", description = "Пингует все серверы в группе")
    public ResponseEntity<Map<Long, Boolean>> pingGroup(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.pingGroup(id));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Запуск скрипта на группе", description = "Создает задачи для всех серверов группы")
    public ResponseEntity<List<TaskDto>> executeScript(
        @PathVariable Long id,
        @RequestParam Long scriptId
    ) {
        return ResponseEntity.ok(groupService.executeScriptOnGroup(id, scriptId));
    }
}