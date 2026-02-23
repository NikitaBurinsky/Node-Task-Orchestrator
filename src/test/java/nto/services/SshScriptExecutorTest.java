package nto.services;

import nto.application.interfaces.repositories.ServerRepository;
import nto.core.entities.ServerEntity;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.services.SshScriptExecutor;
import nto.infrastructure.services.ssh.SshSessionManager;
import org.apache.sshd.client.session.ClientSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SshScriptExecutorTest {

    @Mock
    private JpaTaskRepository taskRepository;
    @Mock
    private TaskStatusCache statusCache;
    @Mock
    private ServerRepository serverRepository;
    @Mock
    private SshSessionManager sessionManager;

    @InjectMocks
    private SshScriptExecutor sshScriptExecutor;

    @Test
    void ping_ShouldReturnTrue_WhenSessionIsOpen() throws Exception {
        Long serverId = 1L;
        ServerEntity server = new ServerEntity();
        server.setId(serverId);
        server.setIpAddress("192.168.1.1");

        ClientSession mockSession = mock(ClientSession.class);

        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));
        when(sessionManager.getOrCreateSession(server)).thenReturn(mockSession);
        when(mockSession.isOpen()).thenReturn(true);

        // Действие
        boolean isAlive = sshScriptExecutor.ping(serverId);

        assertTrue(isAlive, "Пинг должен быть успешен, если сессия открыта");
        // мы не пытались инвалидировать кэш сессии при успехе
        verify(sessionManager, never()).invalidateSession(serverId);
    }

    @Test
    void pingShouldReturnFalseAndInvalidateSessionWhenExceptionOccurs() throws Exception {
        Long serverId = 2L;
        ServerEntity server = new ServerEntity();
        server.setId(serverId);
        server.setIpAddress("10.0.0.1");

        // Настраиваем выброс исключения при попытке получить сессию
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));
        when(sessionManager.getOrCreateSession(server)).thenThrow(
            new RuntimeException("Connection timeout"));

        // Действие
        boolean isAlive = sshScriptExecutor.ping(serverId);

        assertFalse(isAlive, "Пинг должен вернуть false при ошибке соединения");
        verify(sessionManager, times(1)).invalidateSession(serverId);
    }
}