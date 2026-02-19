package nto.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.ScriptExecutor;
import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nto.executor.type", havingValue = "mock", matchIfMissing = true)
public class MockScriptExecutor implements ScriptExecutor {

    private final JpaTaskRepository taskRepository;
    private final TaskStatusCache statusCache;
    private final ServerRepository serverRepository;

    // Счетчики для Лабы 6 (Race Condition Demo)
    private final AtomicLong atomicCounter = new AtomicLong(0);
    private long unsafeCounter = 0; // Не защищен от гонки!

    @Override
    public boolean ping(Long serverId) {
        log.info("[Mock] Pinging server {}", serverId);
        // Проверяем, существует ли сервер вообще
        if (serverRepository.findById(serverId).isEmpty()) {
            log.warn("Server {} not found", serverId);
            return false;
        }

        // Имитация задержки сети
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        // Имитируем успех
        return true;
    }

    @Override
    @Async("taskExecutor") // Запуск в отдельном потоке
    // REQUIRES_NEW: Важно! Создаем НОВУЮ транзакцию, независимую от той, где задача создавалась.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAsync(Long taskId) {
        log.info("Starting execution for Task ID: {}", taskId);

        TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found async: " + taskId));

        // 1. Ставим статус RUNNING
        task.setStartedAt(java.time.LocalDateTime.now());
        updateStatus(task, TaskStatus.RUNNING, "Initializing connection...");

        try {
            // 2. Имитация долгой работы (SSH Handshake + Execution)
            // Задержка от 1 до 3 секунд
            Thread.sleep(1000 + new Random().nextInt(2000));

            // Имитируем вывод скрипта
            String fakeOutput = "Connected to " + task.getServer().getHostname() + "\n" +
                "Executing: " + task.getScript().getName() + "\n" +
                "Done. Exit code 0.";

            // 3. Успешное завершение
            task.setFinishedAt(java.time.LocalDateTime.now());
            updateStatus(task, TaskStatus.SUCCESS, fakeOutput);

            // --- DEMO RACE CONDITION ---
            incrementCounters();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.setFinishedAt(java.time.LocalDateTime.now());
            updateStatus(task, TaskStatus.CANCELLED, "Execution interrupted");
            return;
        } catch (Exception e) {
            log.error("Task failed", e);
            task.setFinishedAt(java.time.LocalDateTime.now());
            updateStatus(task, TaskStatus.FAILED, "Error: " + e.getMessage());
        }
    }

    // Вспомогательный метод обновления
    private void updateStatus(TaskEntity task, TaskStatus status, String output) {
        task.setStatus(status);
        task.setOutput(output);
        TaskEntity saved = taskRepository.save(task);
        statusCache.put(saved); // Обновляем кэш, чтобы клиент видел прогресс
        log.info("Task {} -> {}", task.getId(), status);
    }

    private void incrementCounters() {
        // Потокобезопасно
        atomicCounter.incrementAndGet();

        // ОПАСНО! Чтение-Модификация-Запись не атомарны.
        // При 50+ потоках значения потеряются.
        unsafeCounter++;
    }

    @Override
    public long getSuccessCountAtomic() {
        return atomicCounter.get();
    }

    @Override
    public long getSuccessCountUnsafe() {
        return unsafeCounter;
    }
}