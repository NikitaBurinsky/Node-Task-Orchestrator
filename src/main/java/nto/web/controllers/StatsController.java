package nto.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nto.application.interfaces.services.ScriptExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Статистика и технические метрики")
public class StatsController {

    private final ScriptExecutor scriptExecutor;

    @GetMapping
    @Operation(
        summary = "Получить статистику",
        description = "Возвращает счетчики успешных запусков и разницу между безопасным и небезопасным счетчиком."
    )
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(Map.of(
            "atomic_safe_counter", scriptExecutor.getSuccessCountAtomic(),
            "unsafe_counter", scriptExecutor.getSuccessCountUnsafe(),
            "diff", scriptExecutor.getSuccessCountAtomic() - scriptExecutor.getSuccessCountUnsafe()
        ));
    }
}
