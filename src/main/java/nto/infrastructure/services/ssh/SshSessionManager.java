package nto.infrastructure.services.ssh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nto.core.entities.ServerEntity;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class SshSessionManager {

    private SshClient client;

    private final Map<Long, ClientSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> serverLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.client = SshClient.setUpDefaultClient();
        this.client.start();
        log.info("SSH Client started");
    }

    @PreDestroy
    public void destroy() {
        log.info("Closing all SSH sessions...");
        for (ClientSession session : sessions.values()) {
            if (session.isOpen()) {
                session.close(true);
            }
        }
        sessions.clear();

        if (client != null && client.isStarted()) {
            client.stop();
        }
        log.info("SSH Client stopped");
    }

    public ClientSession getOrCreateSession(ServerEntity server) throws IOException {
        Long serverId = server.getId();

        ReentrantLock lock = serverLocks.computeIfAbsent(serverId, id -> new ReentrantLock());
        lock.lock();
        try {
            ClientSession existingSession = sessions.get(serverId);

            if (existingSession != null && existingSession.isOpen() && !existingSession.isClosed()) {
                return existingSession;
            }

            log.info("Opening new SSH session for server {}:{}", server.getIpAddress(), server.getPort());

            ClientSession newSession = client.connect(
                server.getUsername(),
                server.getIpAddress(),
                server.getPort()
            ).verify(10, TimeUnit.SECONDS).getSession();

            newSession.addPasswordIdentity(server.getPassword());
            newSession.auth().verify(10, TimeUnit.SECONDS);

            sessions.put(serverId, newSession);
            return newSession;

        } finally {
            lock.unlock();
        }
    }

    public void invalidateSession(Long serverId) {
        ClientSession deadSession = sessions.remove(serverId);
        if (deadSession != null && !deadSession.isClosed()) {
            try {
                deadSession.close(true);
            } catch (Exception e) {
                log.warn("Error closing dead session for server {}", serverId);
            }
        }
    }
}