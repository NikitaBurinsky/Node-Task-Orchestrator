package nto.infrastructure.services;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.ScriptExecutor;
import nto.core.entities.ServerEntity;
import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nto.executor.type", havingValue = "ssh")
public class SshScriptExecutor implements ScriptExecutor {

    private final JpaTaskRepository taskRepository;
    private final TaskStatusCache statusCache;
    private final ServerRepository serverRepository;

    // Счетчикi
    private final AtomicLong atomicCounter = new AtomicLong(0);
    private long unsafeCounter = 0;

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAsync(Long taskId) {
        log.info("[SSH] Starting execution for Task ID: {}", taskId);

        TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        task.setStartedAt(java.time.LocalDateTime.now());
        updateStatus(task, TaskStatus.RUNNING, "Connecting via SSH...");

        Session session = null;
        ChannelExec channel = null;

        try {
            // Вынесли настройку сессии
            session = createSshSession(task.getServer());
            session.connect(10000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(task.getScript().getContent());
            channel.setInputStream(null);

            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();

            channel.connect();

            // Вынесли сложный цикл чтения вывода
            String output = readChannelOutput(channel, in, err);

            TaskStatus finalStatus = (channel.getExitStatus() == 0) ? TaskStatus.SUCCESS : TaskStatus.FAILED;
            task.setFinishedAt(java.time.LocalDateTime.now());
            updateStatus(task, finalStatus, output);

            // Обновляем счетчики
            atomicCounter.incrementAndGet();
            ++unsafeCounter;

        } catch (Exception e) {
            log.error("SSH Execution failed", e);
            task.setFinishedAt(java.time.LocalDateTime.now());
            updateStatus(task, TaskStatus.FAILED, "SSH Error: " + e.getMessage());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private Session createSshSession(ServerEntity server) throws Exception {
        JSch jsch = new JSch();
        int sshPort = server.getPort() != null ? server.getPort() : 22;
        Session session = jsch.getSession(server.getUsername(), server.getIpAddress(), sshPort);
        session.setPassword(server.getPassword());
        // Отключаем проверку HostKey для простоты (в проде так нельзя, MITM атака!)
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }

    private String readChannelOutput(ChannelExec channel, InputStream in, InputStream err) throws Exception {
        StringBuilder outputBuffer = new StringBuilder();
        byte[] buffer = new byte[1024];

        while (true) {
            readStreamData(in, buffer, outputBuffer, "");
            readStreamData(err, buffer, outputBuffer, "[ERR] ");

            if (channel.isClosed()) {
                if (in.available() > 0) {
                    continue;
                }
                outputBuffer.append("\nExit Status: ").append(channel.getExitStatus());
                break;
            }
            Thread.sleep(100);
        }
        return outputBuffer.toString();
    }

    private void readStreamData(InputStream stream, byte[] buffer, StringBuilder outputBuffer, String prefix) throws Exception {
        while (stream.available() > 0) {
            int bytesRead = stream.read(buffer, 0, 1024);
            if (bytesRead < 0) {
                break;
            }
            outputBuffer.append(prefix)
                .append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
    }

    @Override
    public boolean ping(Long serverId) {
        log.info("[SSH] Pinging server ID: {}", serverId);

        ServerEntity server = serverRepository.findById(serverId)
            .orElseThrow(() -> new EntityNotFoundException("Server not found: " + serverId));

        Session session = null;
        try {
            JSch jsch = new JSch();
            int sshPort = server.getPort() != null ? server.getPort() : 22;

            session = jsch.getSession(server.getUsername(), server.getIpAddress(), sshPort);
            session.setPassword(server.getPassword());
            session.setConfig("StrictHostKeyChecking",
                "no"); // Внимание: для MVP ок, в проде - known_hosts

            // Пытаемся подключиться с таймаутом 3 секунды (для пинга достаточно)
            session.connect(3000);

            return session.isConnected();
        } catch (Exception e) {
            log.warn("Ping failed for server {}: {}", server.getIpAddress(), e.getMessage());
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void updateStatus(TaskEntity task, TaskStatus status, String output) {
        task.setStatus(status);
        task.setOutput(output);
        TaskEntity saved = taskRepository.save(task);
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
}