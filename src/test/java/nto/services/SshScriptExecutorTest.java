package nto.services;

import jakarta.persistence.EntityNotFoundException;
import nto.application.interfaces.repositories.ServerRepository;
import nto.core.entities.ScriptEntity;
import nto.core.entities.ServerEntity;
import nto.core.entities.SshUsernameEntity;
import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.services.SshScriptExecutor;
import nto.infrastructure.services.ssh.SshSessionManager;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
    void pingShouldReturnTrueWhenSessionIsOpen() throws Exception {
        Long serverId = 1L;
        ServerEntity server = new ServerEntity();
        server.setId(serverId);
        server.setIpAddress("192.168.1.1");

        ClientSession mockSession = mock(ClientSession.class);

        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));
        when(sessionManager.getOrCreateSession(server)).thenReturn(mockSession);
        when(mockSession.isOpen()).thenReturn(true);

        boolean isAlive = sshScriptExecutor.ping(serverId);

        assertTrue(isAlive);
        verify(sessionManager, never()).invalidateSession(serverId);
    }

    @Test
    void pingShouldReturnFalseAndInvalidateSessionWhenExceptionOccurs() throws Exception {
        Long serverId = 2L;
        ServerEntity server = new ServerEntity();
        server.setId(serverId);
        server.setIpAddress("10.0.0.1");

        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));
        when(sessionManager.getOrCreateSession(server)).thenThrow(new RuntimeException("Connection timeout"));

        boolean isAlive = sshScriptExecutor.ping(serverId);

        assertFalse(isAlive);
        verify(sessionManager, times(1)).invalidateSession(serverId);
    }

    @Test
    void pingShouldThrowWhenServerDoesNotExist() {
        when(serverRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> sshScriptExecutor.ping(5L));
    }

    @Test
    void executeAsyncShouldMarkTaskAsSuccessAndUpdateCounters() throws Exception {
        TaskEntity task = taskWithIds(9L, 100L);
        ClientSession session = mock(ClientSession.class);
        ChannelExec channel = mock(ChannelExec.class);
        OpenFuture openFuture = mock(OpenFuture.class);

        when(statusCache.get(9L)).thenReturn(null);
        when(taskRepository.findById(9L)).thenReturn(Optional.of(task));
        CopyOnWriteArrayList<TaskStatus> savedStatuses = new CopyOnWriteArrayList<>();
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });
        when(sessionManager.getOrCreateSession(task.getServer())).thenReturn(session);
        when(session.createExecChannel("echo ok")).thenReturn(channel);
        when(channel.open()).thenReturn(openFuture);
        when(openFuture.verify(eq(15L), eq(TimeUnit.SECONDS))).thenReturn(openFuture);
        when(channel.waitFor(eq(EnumSet.of(ClientChannelEvent.CLOSED)), eq(0L)))
            .thenReturn(EnumSet.of(ClientChannelEvent.CLOSED));
        when(channel.getExitStatus()).thenReturn(0);

        sshScriptExecutor.executeAsync(9L);

        assertEquals(List.of(TaskStatus.RUNNING, TaskStatus.SUCCESS), savedStatuses);
        assertEquals(1L, sshScriptExecutor.getSuccessCountAtomic());
        assertEquals(1L, sshScriptExecutor.getSuccessCountUnsafe());
        verify(statusCache, times(2)).put(any(TaskEntity.class));
    }

    @Test
    void executeAsyncShouldFailAndInvalidateSessionOnSshError() throws Exception {
        TaskEntity task = taskWithIds(10L, 101L);

        when(statusCache.get(10L)).thenReturn(task);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        CopyOnWriteArrayList<TaskStatus> savedStatuses = new CopyOnWriteArrayList<>();
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });
        when(sessionManager.getOrCreateSession(task.getServer())).thenThrow(new RuntimeException("no route"));

        sshScriptExecutor.executeAsync(10L);

        assertEquals(List.of(TaskStatus.RUNNING, TaskStatus.FAILED), savedStatuses);
        verify(sessionManager).invalidateSession(101L);
        verify(statusCache, times(2)).put(any(TaskEntity.class));
    }

    @Test
    void executeAsyncShouldInvalidateSessionWhenChannelExecutionFails() throws Exception {
        TaskEntity task = taskWithIds(13L, 103L);
        ClientSession session = mock(ClientSession.class);
        ChannelExec channel = mock(ChannelExec.class);
        CopyOnWriteArrayList<TaskStatus> savedStatuses = new CopyOnWriteArrayList<>();

        when(statusCache.get(13L)).thenReturn(task);
        when(taskRepository.findById(13L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });
        when(sessionManager.getOrCreateSession(task.getServer())).thenReturn(session);
        when(session.createExecChannel("echo ok")).thenReturn(channel);
        when(channel.open()).thenThrow(new RuntimeException("open failed"));

        sshScriptExecutor.executeAsync(13L);

        assertEquals(List.of(TaskStatus.RUNNING, TaskStatus.FAILED), savedStatuses);
        verify(sessionManager, times(2)).invalidateSession(103L);
    }

    @Test
    void executeAsyncShouldFailWhenExitStatusMissingAndCombineOutputs() throws Exception {
        TaskEntity task = taskWithIds(12L, 102L);
        ClientSession session = mock(ClientSession.class);
        ChannelExec channel = mock(ChannelExec.class);
        OpenFuture openFuture = mock(OpenFuture.class);
        CopyOnWriteArrayList<TaskStatus> savedStatuses = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<String> savedOutputs = new CopyOnWriteArrayList<>();

        when(statusCache.get(12L)).thenReturn(task);
        when(taskRepository.findById(12L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity saved = invocation.getArgument(0);
            savedStatuses.add(saved.getStatus());
            savedOutputs.add(saved.getOutput());
            return saved;
        });
        when(sessionManager.getOrCreateSession(task.getServer())).thenReturn(session);
        when(session.createExecChannel("echo ok")).thenReturn(channel);
        doAnswer(invocation -> {
            ((ByteArrayOutputStream) invocation.getArgument(0))
                .write("stdout".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(channel).setOut(any(ByteArrayOutputStream.class));
        doAnswer(invocation -> {
            ((ByteArrayOutputStream) invocation.getArgument(0))
                .write("stderr".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(channel).setErr(any(ByteArrayOutputStream.class));
        when(channel.open()).thenReturn(openFuture);
        when(openFuture.verify(eq(15L), eq(TimeUnit.SECONDS))).thenReturn(openFuture);
        when(channel.waitFor(eq(EnumSet.of(ClientChannelEvent.CLOSED)), eq(0L)))
            .thenReturn(EnumSet.of(ClientChannelEvent.CLOSED));
        when(channel.getExitStatus()).thenReturn(null);

        sshScriptExecutor.executeAsync(12L);

        assertEquals(List.of(TaskStatus.RUNNING, TaskStatus.FAILED), savedStatuses);
        assertTrue(savedOutputs.get(1).contains("stdout"));
        assertTrue(savedOutputs.get(1).contains("[ERR] stderr"));
        assertTrue(savedOutputs.get(1).contains("Exit Status: -1"));
    }

    @Test
    void executeAsyncShouldFormatOnlyStderrWithoutLeadingBlankLine() throws Exception {
        TaskEntity task = taskWithIds(14L, 104L);
        ClientSession session = mock(ClientSession.class);
        ChannelExec channel = mock(ChannelExec.class);
        OpenFuture openFuture = mock(OpenFuture.class);
        CopyOnWriteArrayList<String> savedOutputs = new CopyOnWriteArrayList<>();

        when(statusCache.get(14L)).thenReturn(task);
        when(taskRepository.findById(14L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity saved = invocation.getArgument(0);
            savedOutputs.add(saved.getOutput());
            return saved;
        });
        when(sessionManager.getOrCreateSession(task.getServer())).thenReturn(session);
        when(session.createExecChannel("echo ok")).thenReturn(channel);
        doAnswer(invocation -> {
            ((ByteArrayOutputStream) invocation.getArgument(0))
                .write("stderr-only".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(channel).setErr(any(ByteArrayOutputStream.class));
        when(channel.open()).thenReturn(openFuture);
        when(openFuture.verify(eq(15L), eq(TimeUnit.SECONDS))).thenReturn(openFuture);
        when(channel.waitFor(eq(EnumSet.of(ClientChannelEvent.CLOSED)), eq(0L)))
            .thenReturn(EnumSet.of(ClientChannelEvent.CLOSED));
        when(channel.getExitStatus()).thenReturn(1);

        sshScriptExecutor.executeAsync(14L);

        assertEquals("[ERR] stderr-only\nExit Status: 1", savedOutputs.get(1));
    }

    @Test
    void executeAsyncShouldThrowWhenTaskMissingDuringStatusUpdate() {
        when(statusCache.get(12L)).thenReturn(taskWithIds(12L, 102L));
        when(taskRepository.findById(12L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> sshScriptExecutor.executeAsync(12L));
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    @Test
    void executeAsyncShouldThrowWhenTaskIsAbsentDuringPreparation() {
        when(statusCache.get(11L)).thenReturn(null);
        when(taskRepository.findById(11L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> sshScriptExecutor.executeAsync(11L));
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    private TaskEntity taskWithIds(Long taskId, Long serverId) {
        ServerEntity server = new ServerEntity();
        server.setId(serverId);
        server.setHostname("srv");
        server.setIpAddress("127.0.0.1");
        server.setPort(22);
        server.setPassword("pw");
        server.setSshUsername(SshUsernameEntity.builder().username("root").build());

        ScriptEntity script = ScriptEntity.builder()
            .id(44L)
            .name("s")
            .content("echo ok")
            .isPublic(true)
            .build();

        return TaskEntity.builder()
            .id(taskId)
            .server(server)
            .script(script)
            .status(TaskStatus.PENDING)
            .build();
    }
}
