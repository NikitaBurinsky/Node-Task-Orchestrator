package nto.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerDto;
import nto.core.entities.ServerEntity;
import nto.application.interfaces.services.ServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@Tag(name = "Servers", description = "Управление серверами")
public class ServerController {

    private final ServerService serverService;

    @PostMapping
    @Operation(
        summary = "Создать сервер",
        description = "Добавляет новый сервер и возвращает его данные."
    )
    public ResponseEntity<ServerDto> create(@RequestBody @Valid ServerDto dto) {
        return ResponseEntity.ok(serverService.createServer(dto));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Получить сервер",
        description = "Возвращает данные сервера по его идентификатору."
    )
    public ResponseEntity<ServerDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serverService.getServerById(id));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить сервер",
        description = "Обновляет параметры сервера по идентификатору."
    )
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @RequestBody ServerDto serverDto) {
        serverService.updateServer(id, serverDto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить сервер",
        description = "Удаляет сервер по идентификатору."
    )
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
        summary = "Список серверов",
        description = "Возвращает список всех серверов."
    )
    public ResponseEntity<List<ServerDto>> getAll(@RequestParam(required = false) String hostname) {
        return ResponseEntity.ok(serverService.getAllServers().stream().filter((ServerDto s) -> s.hostname().contains(hostname)).toList());
    }

    @GetMapping("/{id}/ping")
    @Operation(summary = "Проверка доступности", description = "Пытается установить SSH соединение с сервером")
    public ResponseEntity<Map<String, Object>> pingServer(@PathVariable Long id) {
        boolean isAlive = serverService.checkConnection(id);

        return ResponseEntity.ok(Map.of(
            "serverId", id,
            "alive", isAlive,
            "timestamp", java.time.LocalDateTime.now()
        ));
    }
}
