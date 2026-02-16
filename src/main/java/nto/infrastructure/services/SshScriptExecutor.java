package nto.infrastructure.services;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Счетчикi
    private final AtomicLong atomicCounter = new AtomicLong(0);
    private long unsafeCounter = 0;

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAsync(Long taskId) {
        log.info("[SSH] Starting execution for Task ID: {}", taskId);

        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        updateStatus(task, TaskStatus.RUNNING, "Connecting via SSH...");

        Session session = null;
        ChannelExec channel = null;

        try {
            ServerEntity server = task.getServer();
            JSch jsch = new JSch();

            // 1. Настройка сессии
            // Порт по умолчанию 22, если не задан
            int sshPort = server.getPort() != null ? server.getPort() : 22;
            session = jsch.getSession(server.getUsername(), server.getIpAddress(), sshPort);
            session.setPassword(server.getPassword());

            // Отключаем проверку HostKey для простоты (в проде так нельзя, MITM атака!)
            session.setConfig("StrictHostKeyChecking", "no");
            // Таймаут подключения 10 сек
            session.connect(10000);

            // 2. Открытие канала для выполнения команды
            channel = (ChannelExec) session.openChannel("exec");

            // Вставляем скрипт.
            // ВАЖНО: В реальной жизни нужно экранировать.
            // Сейчас просто передаем content. Лучше обернуть в bash -c '...'
            channel.setCommand(task.getScript().getContent());

            channel.setInputStream(null);

            // Получаем потоки вывода (stdout + stderr)
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();

            // 3. Запуск
            channel.connect();

            // 4. Чтение вывода
            StringBuilder outputBuffer = new StringBuilder();
            byte[] tmp = new byte[1024];

            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    outputBuffer.append(new String(tmp, 0, i, StandardCharsets.UTF_8));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    outputBuffer.append("[ERR] ").append(new String(tmp, 0, i, StandardCharsets.UTF_8));
                }

                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    outputBuffer.append("\nExit Status: ").append(channel.getExitStatus());
                    break;
                }
                Thread.sleep(100); // Небольшая пауза, чтобы не грузить CPU
            }

            // 5. Обработка результата
            TaskStatus finalStatus = (channel.getExitStatus() == 0) ? TaskStatus.SUCCESS : TaskStatus.FAILED;
            updateStatus(task, finalStatus, outputBuffer.toString());

            // Обновляем счетчики
            atomicCounter.incrementAndGet();
            unsafeCounter++;

        } catch (Exception e) {
            log.error("SSH Execution failed", e);
            updateStatus(task, TaskStatus.FAILED, "SSH Error: " + e.getMessage());
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
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