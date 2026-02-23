package nto.services;

import nto.application.dto.TaskDto;
import nto.application.interfaces.repositories.ScriptRepository;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.core.enums.TaskStatus;
import nto.core.utils.exceptions.ServerBusyException;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.services.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private JpaTaskRepository taskRepository;
    @Mock private TaskStatusCache statusCache;
    @Mock private MappingService mappingService;
    @Mock private ServerRepository serverRepository;
    @Mock private ScriptRepository scriptRepository;
    @Mock private ScriptExecutor scriptExecutor;

    @InjectMocks
    private TaskServiceImpl taskService;

    private final String TEST_USERNAME = "test_user";

    @BeforeEach
    void setUpSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(TEST_USERNAME);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createTask_ShouldThrowServerBusyException_WhenServerHasActiveTasks() {
        TaskDto inputDto = new TaskDto(1L, TaskStatus.PENDING, null, null, null, null, null, null);
        List<TaskStatus> activeStatuses = List.of(TaskStatus.PENDING, TaskStatus.RUNNING);

        when(taskRepository.existsByServerIdAndStatusIn(inputDto.serverId(), activeStatuses))
            .thenReturn(true);

        assertThrows(ServerBusyException.class, () -> taskService.createTask(inputDto),
            "Должна выбрасываться ошибка, если управляемый сервер занят");

        // не пошли дальше по коду
        verify(scriptRepository, never()).findById(any());
    }
}