package nto.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nto.application.dto.ScriptDto;
import nto.application.interfaces.services.ScriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scripts")
@RequiredArgsConstructor
@Tag(name = "Scripts", description = "Управление библиотекой скриптов")
public class ScriptController {

    private final ScriptService scriptService;

    @PostMapping
    @Operation(summary = "Создать скрипт", description = "Добавляет новый bash/shell скрипт в библиотеку")
    public ResponseEntity<ScriptDto> create(@RequestBody @Valid ScriptDto dto) {
        return ResponseEntity.ok(scriptService.createScript(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить скрипт", description = "Возвращает тело скрипта и метаданные по ID")
    public ResponseEntity<ScriptDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scriptService.getScriptById(id));
    }

    @GetMapping
    @Operation(summary = "Все скрипты", description = "Список всех доступных скриптов")
    public ResponseEntity<List<ScriptDto>> getAll() {
        return ResponseEntity.ok(scriptService.getAllScripts());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить скрипт", description = "Удаляет скрипт из БД. Осторожно: удалит связанные Task, если настроен Cascade!")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scriptService.deleteScript(id);
        return ResponseEntity.noContent().build();
    }
}