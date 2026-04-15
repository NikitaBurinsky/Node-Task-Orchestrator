package nto.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import nto.application.interfaces.repositories.ServerRepository;
import nto.core.entities.ScriptEntity;
import nto.core.entities.ServerEntity;
import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.services.MockScriptExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockScriptExecutorTest {

    @Mock
    private JpaTaskRepository taskRepository;
    @Mock
    private TaskStatusCache statusCache;
    @Mock
    private ServerRepository serverRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MockScriptExecutor mockScriptExecutor;

    @Test
    void pingShouldReturnTrueWhenServerExists() {
        when(serverRepository.findById(1L)).thenReturn(Optional.of(new ServerEntity()));

        boolean result = mockScriptExecutor.ping(1L);

        assertTrue(result);
    }

    @Test
    void pingShouldReturnFalseWhenServerMissing() {
        when(serverRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = mockScriptExecutor.ping(1L);

        assertFalse(result);
    }

    @Test
    void executeAsyncShouldSetRunningThenSuccessAndUpdateCounters() {
        TaskEntity task = TaskEntity.builder()
            .id(10L)
            .server(ServerEntity.builder().hostname("srv").build())
            .script(ScriptEntity.builder().name("deploy").build())
            .status(TaskStatus.PENDING)
            .build();

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        CopyOnWriteArrayList<TaskStatus> savedStatuses = new CopyOnWriteArrayList<>();
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> {
            TaskEntity saved = inv.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });

        mockScriptExecutor.executeAsync(10L);

        assertEquals(List.of(TaskStatus.RUNNING, TaskStatus.SUCCESS), savedStatuses);
        verify(entityManager, times(2)).merge(task);
        verify(statusCache, times(2)).put(any(TaskEntity.class));
        assertEquals(1L, mockScriptExecutor.getSuccessCountAtomic());
        assertEquals(1L, mockScriptExecutor.getSuccessCountUnsafe());
    }

    @Test
    void executeAsyncShouldSetFailedWhenExecutionThrows() {
        TaskEntity task = TaskEntity.builder()
            .id(10L)
            .server(null)
            .script(ScriptEntity.builder().name("deploy").build())
            .status(TaskStatus.PENDING)
            .build();

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        CopyOnWriteArrayList<TaskStatus> savedStatuses = new CopyOnWriteArrayList<>();
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> {
            TaskEntity saved = inv.getArgument(0);
            savedStatuses.add(saved.getStatus());
            return saved;
        });

        mockScriptExecutor.executeAsync(10L);

        assertEquals(List.of(TaskStatus.RUNNING, TaskStatus.FAILED), savedStatuses);
        verify(entityManager, times(2)).merge(task);
    }

    @Test
    void executeAsyncShouldThrowWhenTaskMissing() {
        when(taskRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mockScriptExecutor.executeAsync(77L));
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    @Test
    void executeAsyncShouldPreserveInterruptFlagWhenSleepIsInterrupted() {
        TaskEntity task = TaskEntity.builder()
            .id(12L)
            .server(ServerEntity.builder().hostname("srv").build())
            .script(ScriptEntity.builder().name("deploy").build())
            .status(TaskStatus.PENDING)
            .build();

        when(taskRepository.findById(12L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        try {
            Thread.currentThread().interrupt();

            mockScriptExecutor.executeAsync(12L);

            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
