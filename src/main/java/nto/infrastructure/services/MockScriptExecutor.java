package nto.infrastructure.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nto.executor.type", havingValue = "mock", matchIfMissing = true)
public class MockScriptExecutor implements ScriptExecutor {

    private final JpaTaskRepository taskRepository;
    private final TaskStatusCache statusCache;
    private final ServerRepository serverRepository;
    @PersistenceContext
    private final EntityManager entityManager;

    // Счетчики для Лабы 6 (Race Condition Demo)
    private final AtomicLong atomicCounter = new AtomicLong(0);
    private long unsafeCounter = 0; // Не защищен от гонки!

    @Override
    public boolean ping(Long serverId) {
        log.info("[Mock] Pinging server {}", serverId);
        if (serverRepository.findById(serverId).isEmpty()) {
            log.warn("Server {} not found", serverId);
            return false;
        }
        return true;
    }

    @Override
    @Async("taskExecutor") // Запуск в отдельном потоке
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAsync(Long taskId) {
        log.info("Starting execution for Task ID: {}", taskId);
        incrementCounters();
        TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found async: " + taskId));

        // 1. Ставим статус RUNNING
        task.setStartedAt(java.time.LocalDateTime.now());
        updateStatus(task, TaskStatus.RUNNING, "Initializing connection...");

        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            String fakeOutput = "Connected to " + task.getServer().getHostname() + "\n" +
                "Executing: " + task.getScript().getName() + "\n" +
                "Done. Exit code 0.";

            // 3. Успешное завершение
            task.setFinishedAt(java.time.LocalDateTime.now());
            updateStatus(task, TaskStatus.SUCCESS, fakeOutput);
        } catch (Exception e) {
            log.error("Task failed", e);
            task.setFinishedAt(java.time.LocalDateTime.now());
            updateStatus(task, TaskStatus.FAILED, "Error: " + e.getMessage());
        }
    }

    // Вспомогательный метод обновления
    private void updateStatus(TaskEntity task, TaskStatus status, String output) {
        entityManager.merge(task);
        task.setStatus(status);
        task.setOutput(output);
        TaskEntity saved = taskRepository.save(task);
        statusCache.put(saved);
        log.info("Task {} -> {}", task.getId(), status);
    }

    private void incrementCounters() {
        atomicCounter.incrementAndGet();
        long temp = unsafeCounter;
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        unsafeCounter = temp + 1;
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