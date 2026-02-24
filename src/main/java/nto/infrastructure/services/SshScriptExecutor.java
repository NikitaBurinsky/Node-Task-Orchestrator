package nto.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.ScriptExecutor;
import nto.core.entities.ServerEntity;
import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import nto.core.utils.ErrorMessages;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.services.ssh.SshSessionManager;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.springdoc.api.ErrorMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nto.executor.type", havingValue = "ssh")
public class SshScriptExecutor implements ScriptExecutor {
    private final JpaTaskRepository taskRepository;
    private final TaskStatusCache statusCache;
    private final ServerRepository serverRepository;
    // Внедряем наш новый менеджер сессий
    private final SshSessionManager sessionManager;
    // Счетчики (для демонстрации Race Condition в Лабе 6)
    private final AtomicLong atomicCounter = new AtomicLong(0);
    private long unsafeCounter = 0;

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAsync(Long taskId) {
        TaskEntity task = prepareTask(taskId);
        if (task == null) {
            return;
        }

        try {
            ExecutionResult result = performSshExecution(task);
            finalizeTask(task, result.status(), result.output());
            updateMetrics();
        } catch (Exception e) {
            handleExecutionError(task, e);
        }
    }

    @Override
    public boolean ping(Long serverId) {
        log.info("[SSH] Pinging server ID: {}", serverId);

        ServerEntity server = serverRepository.findById(serverId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage() + serverId));

        try {
            ClientSession session = sessionManager.getOrCreateSession(server);
            return session.isOpen();
        } catch (Exception e) {
            log.warn("Ping failed for server {}: {}", server.getIpAddress(), e.getMessage());
            // Если пинг упал, возможно сервер перезагрузился, сбрасываем кэш сессии
            sessionManager.invalidateSession(serverId);
            return false;
        }
    }

    private TaskEntity prepareTask(Long taskId) {
        log.info("[SSH] Preparing Task ID: {}", taskId);
        TaskEntity task = statusCache.get(taskId);
        if (task == null) {
            task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        }

        task.setStartedAt(java.time.LocalDateTime.now());
        updateStatus(task.getId(), TaskStatus.RUNNING, "Executing via SSH pool...");
        return task;
    }

    private ExecutionResult performSshExecution(TaskEntity task) throws Exception {
        ServerEntity server = task.getServer();
        String script = task.getScript().getContent();

        ClientSession session = sessionManager.getOrCreateSession(server);

        try (ChannelExec channel = session.createExecChannel(script);
             ByteArrayOutputStream stdout = new ByteArrayOutputStream();
             ByteArrayOutputStream stderr = new ByteArrayOutputStream()) {

            channel.setOut(stdout);
            channel.setErr(stderr);
            channel.open().verify(15, TimeUnit.SECONDS);

            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0L);

            int exitCode = (channel.getExitStatus() != null) ? channel.getExitStatus() : -1;
            String combinedOutput = formatOutput(stdout, stderr, exitCode);
            TaskStatus status = (exitCode == 0) ? TaskStatus.SUCCESS : TaskStatus.FAILED;

            return new ExecutionResult(status, combinedOutput);
        } catch (Exception e) {
            sessionManager.invalidateSession(server.getId());
            throw e;
        }
    }

    private String formatOutput(ByteArrayOutputStream out, ByteArrayOutputStream err,
                                int exitCode) {
        StringBuilder sb = new StringBuilder();
        String stdOutStr = out.toString(StandardCharsets.UTF_8);
        String stdErrStr = err.toString(StandardCharsets.UTF_8);

        if (!stdOutStr.isEmpty()) {
            sb.append(stdOutStr);
        }
        if (!stdErrStr.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("[ERR] ").append(stdErrStr);
        }
        sb.append("\nExit Status: ").append(exitCode);
        return sb.toString();
    }

    private void finalizeTask(TaskEntity task, TaskStatus status, String output) {
        task.setFinishedAt(java.time.LocalDateTime.now());
        updateStatus(task.getId(), status, output);
        log.info("[SSH] Task ID: {} finished with status: {}", task.getId(), status);
    }

    private void handleExecutionError(TaskEntity task, Exception e) {
        log.error("[SSH] Critical error during Task ID: {}", task.getId(), e);
        sessionManager.invalidateSession(task.getServer().getId());
        task.setFinishedAt(java.time.LocalDateTime.now());
        updateStatus(task.getId(), TaskStatus.FAILED, "SSH Error: " + e.getMessage());
    }

    private void updateMetrics() {
        atomicCounter.incrementAndGet();
        unsafeCounter++;
    }

    private void updateStatus(Long taskId, TaskStatus status, String output) {
        log.info("[SSH] _+ Task ID: {} updated to status: {}", taskId, status);
        TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        task.setStatus(status);
        task.setOutput(output);

        TaskEntity saved = taskRepository.save(task);
        log.info("[SSH] _+ Saved Task ID: {} status: {}", saved.getId(), status);
        statusCache.put(saved);
    }

    @Override
    public long getSuccessCountAtomic() {
        return atomicCounter.get();
    }

    @Override
    public long getSuccessCountUnsafe() {
        return unsafeCounter;
    }

    private record ExecutionResult(TaskStatus status, String output) {
    }
}