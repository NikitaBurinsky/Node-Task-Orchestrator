package nto.services;

import jakarta.persistence.EntityNotFoundException;
import nto.application.dto.BulkTaskRequestDto;
import nto.application.dto.TaskDto;
import nto.application.interfaces.repositories.ScriptRepository;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.core.entities.ScriptEntity;
import nto.core.entities.ServerEntity;
import nto.core.entities.ServerGroupEntity;
import nto.core.entities.TaskEntity;
import nto.core.entities.UserEntity;
import nto.core.enums.TaskStatus;
import nto.core.utils.exceptions.BadRequestException;
import nto.core.utils.exceptions.ServerBusyException;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.services.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    private static final String TEST_USER = "test_user";

    @Mock
    private JpaTaskRepository taskRepository;
    @Mock
    private TaskStatusCache statusCache;
    @Mock
    private MappingService mappingService;
    @Mock
    private ServerRepository serverRepository;
    @Mock
    private ScriptRepository scriptRepository;
    @Mock
    private ScriptExecutor scriptExecutor;
    @InjectMocks
    private TaskServiceImpl taskService;

    @BeforeEach
    void setUpSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(TEST_USER);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        ReflectionTestUtils.setField(taskService, "executorType", "mock");
    }

    @Test
    void createTaskShouldThrowServerBusyExceptionWhenServerHasActiveTasks() {
        ReflectionTestUtils.setField(taskService, "executorType", "ssh");

        TaskDto inputDto = new TaskDto(null, TaskStatus.PENDING, null, 10L, 20L, null, null, null);
        List<TaskStatus> activeStatuses = List.of(TaskStatus.PENDING, TaskStatus.RUNNING);

        when(taskRepository.existsByServerIdAndStatusIn(inputDto.serverId(), activeStatuses))
            .thenReturn(true);

        assertThrows(ServerBusyException.class, () -> taskService.createTask(inputDto));

        verify(scriptRepository, never()).findById(any());
    }

    @Test
    void createTaskShouldCreateAndStartTaskWhenDataIsValid() {
        TaskDto inputDto = new TaskDto(null, TaskStatus.PENDING, null, 11L, 22L, null, null, null);
        ScriptEntity script = scriptOwnedBy(TEST_USER, false);
        script.setId(22L);
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(11L);

        TaskEntity savedTask = TaskEntity.builder()
            .id(55L)
            .server(server)
            .script(script)
            .status(TaskStatus.PENDING)
            .build();

        TaskDto expectedDto = new TaskDto(55L, TaskStatus.PENDING, null, 11L, 22L, null, null, null);

        when(scriptRepository.findById(22L)).thenReturn(Optional.of(script));
        when(serverRepository.findById(11L)).thenReturn(Optional.of(server));
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(savedTask);
        when(mappingService.mapToDto(savedTask, TaskDto.class)).thenReturn(expectedDto);

        TaskDto result = taskService.createTask(inputDto);

        assertEquals(expectedDto, result);
        verify(statusCache).put(savedTask);
        verify(scriptExecutor).executeAsync(55L);
    }

    @Test
    void createTaskShouldThrowWhenScriptIsPrivateAndNotOwned() {
        TaskDto inputDto = new TaskDto(null, TaskStatus.PENDING, null, 11L, 33L, null, null, null);
        ScriptEntity privateScript = scriptOwnedBy("other", false);

        when(scriptRepository.findById(33L)).thenReturn(Optional.of(privateScript));

        assertThrows(AccessDeniedException.class, () -> taskService.createTask(inputDto));
        verify(serverRepository, never()).findById(any());
    }

    @Test
    void createTaskShouldThrowWhenServerIsNotOwned() {
        TaskDto inputDto = new TaskDto(null, TaskStatus.PENDING, null, 11L, 22L, null, null, null);
        ScriptEntity script = scriptOwnedBy(TEST_USER, false);
        script.setId(22L);
        ServerEntity foreignServer = serverOwnedBy("other");
        foreignServer.setId(11L);

        when(scriptRepository.findById(22L)).thenReturn(Optional.of(script));
        when(serverRepository.findById(11L)).thenReturn(Optional.of(foreignServer));

        assertThrows(AccessDeniedException.class, () -> taskService.createTask(inputDto));
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    @Test
    void createTaskShouldThrowWhenScriptNotFound() {
        TaskDto inputDto = new TaskDto(null, TaskStatus.PENDING, null, 11L, 22L, null, null, null);
        when(scriptRepository.findById(22L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> taskService.createTask(inputDto));
    }

    @Test
    void getTasksWithFiltersShouldMapPageContent() {
        TaskEntity entity = TaskEntity.builder().id(1L).status(TaskStatus.SUCCESS).build();
        TaskDto dto = new TaskDto(1L, TaskStatus.SUCCESS, "ok", 1L, 2L, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(taskRepository.findTasksByUserAndStatusJPQL(TEST_USER, TaskStatus.SUCCESS, pageable))
            .thenReturn(page);
        when(mappingService.mapToDto(entity, TaskDto.class)).thenReturn(dto);

        Page<TaskDto> result = taskService.getTasksWithFilters(TEST_USER, TaskStatus.SUCCESS, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(dto, result.getContent().getFirst());
    }

    @Test
    void getLastStatusShouldReturnFromCacheWhenPresent() {
        ScriptEntity script = scriptOwnedBy(TEST_USER, false);
        script.setId(5L);
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(7L);
        TaskEntity cached = TaskEntity.builder()
            .id(9L)
            .server(server)
            .script(script)
            .status(TaskStatus.RUNNING)
            .build();
        TaskDto dto = new TaskDto(9L, TaskStatus.RUNNING, null, 7L, 5L, null, null, null);

        when(serverRepository.findById(7L)).thenReturn(Optional.of(server));
        when(scriptRepository.findById(5L)).thenReturn(Optional.of(script));
        when(statusCache.get(7L, 5L)).thenReturn(cached);
        when(mappingService.mapToDto(cached, TaskDto.class)).thenReturn(dto);

        TaskDto result = taskService.getLastStatus(7L, 5L);

        assertEquals(dto, result);
        verify(taskRepository, never()).findFirstByServerIdAndScriptIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getLastStatusShouldReadRepositoryWhenCacheMiss() {
        ScriptEntity script = scriptOwnedBy(TEST_USER, false);
        script.setId(5L);
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(7L);
        TaskEntity latest = TaskEntity.builder()
            .id(10L)
            .server(server)
            .script(script)
            .status(TaskStatus.SUCCESS)
            .build();
        TaskDto dto = new TaskDto(10L, TaskStatus.SUCCESS, "done", 7L, 5L, null, null, null);

        when(serverRepository.findById(7L)).thenReturn(Optional.of(server));
        when(scriptRepository.findById(5L)).thenReturn(Optional.of(script));
        when(statusCache.get(7L, 5L)).thenReturn(null);
        when(taskRepository.findFirstByServerIdAndScriptIdOrderByCreatedAtDesc(7L, 5L))
            .thenReturn(Optional.of(latest));
        when(mappingService.mapToDto(latest, TaskDto.class)).thenReturn(dto);

        TaskDto result = taskService.getLastStatus(7L, 5L);

        assertEquals(dto, result);
    }

    @Test
    void getLastStatusShouldReturnNullWhenNothingFound() {
        ScriptEntity script = scriptOwnedBy(TEST_USER, false);
        script.setId(5L);
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(7L);

        when(serverRepository.findById(7L)).thenReturn(Optional.of(server));
        when(scriptRepository.findById(5L)).thenReturn(Optional.of(script));
        when(statusCache.get(7L, 5L)).thenReturn(null);
        when(taskRepository.findFirstByServerIdAndScriptIdOrderByCreatedAtDesc(7L, 5L))
            .thenReturn(Optional.empty());

        TaskDto result = taskService.getLastStatus(7L, 5L);

        assertNull(result);
    }

    @Test
    void createTasksBulkShouldThrowWhenServerListIsIncomplete() {
        BulkTaskRequestDto dto = new BulkTaskRequestDto(8L, List.of(1L, 2L));
        ScriptEntity script = scriptOwnedBy(TEST_USER, true);

        ServerEntity onlyFound = serverOwnedBy(TEST_USER);
        onlyFound.setId(1L);

        when(scriptRepository.findById(8L)).thenReturn(Optional.of(script));
        when(serverRepository.findAllById(dto.serverIds())).thenReturn(List.of(onlyFound));

        assertThrows(BadRequestException.class, () -> taskService.createTasksBulk(dto));
        verify(taskRepository, never()).saveAll(any());
    }

    @Test
    void createTasksBulkShouldThrowWhenExecutorIsNotMockAndServerIsNotOwned() {
        ReflectionTestUtils.setField(taskService, "executorType", "ssh");

        BulkTaskRequestDto dto = new BulkTaskRequestDto(8L, List.of(1L));
        ScriptEntity script = scriptOwnedBy(TEST_USER, false);
        ServerEntity foreignServer = serverOwnedBy("other");
        foreignServer.setId(1L);

        when(scriptRepository.findById(8L)).thenReturn(Optional.of(script));
        when(serverRepository.findAllById(dto.serverIds())).thenReturn(List.of(foreignServer));

        assertThrows(AccessDeniedException.class, () -> taskService.createTasksBulk(dto));
        verify(taskRepository, never()).saveAll(any());
    }

    @Test
    void createTasksBulkShouldCreateTasksAndTriggerExecutor() {
        BulkTaskRequestDto dto = new BulkTaskRequestDto(8L, List.of(1L, 2L));
        ScriptEntity script = scriptOwnedBy(TEST_USER, false);
        script.setId(8L);

        ServerEntity serverOne = serverOwnedBy(TEST_USER);
        serverOne.setId(1L);
        serverOne.setHostname("srv-1");
        ServerEntity serverTwo = serverOwnedBy(TEST_USER);
        serverTwo.setId(2L);
        serverTwo.setHostname("srv-2");

        TaskEntity taskOne = TaskEntity.builder()
            .id(100L)
            .script(script)
            .server(serverOne)
            .status(TaskStatus.PENDING)
            .build();
        TaskEntity taskTwo = TaskEntity.builder()
            .id(101L)
            .script(script)
            .server(serverTwo)
            .status(TaskStatus.PENDING)
            .build();

        List<TaskDto> mapped = List.of(
            new TaskDto(100L, TaskStatus.PENDING, null, 1L, 8L, null, null, null),
            new TaskDto(101L, TaskStatus.PENDING, null, 2L, 8L, null, null, null)
        );

        when(scriptRepository.findById(8L)).thenReturn(Optional.of(script));
        when(serverRepository.findAllById(dto.serverIds())).thenReturn(List.of(serverOne, serverTwo));
        when(taskRepository.saveAll(any())).thenReturn(List.of(taskOne, taskTwo));
        when(mappingService.mapListToDto(List.of(taskOne, taskTwo), TaskDto.class)).thenReturn(mapped);

        List<TaskDto> result = taskService.createTasksBulk(dto);

        assertEquals(mapped, result);
        verify(statusCache).put(taskOne);
        verify(statusCache).put(taskTwo);
        verify(scriptExecutor).executeAsync(100L);
        verify(scriptExecutor).executeAsync(101L);

        ArgumentCaptor<List<TaskEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void getTaskByIdShouldThrowWhenTaskDoesNotExist() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> taskService.getTaskById(42L));
        verify(statusCache, never()).put(any(TaskEntity.class));
    }

    @Test
    void getTaskByIdShouldPutToCacheAndMap() {
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(1L);
        ScriptEntity script = scriptOwnedBy(TEST_USER, false);
        script.setId(2L);
        TaskEntity task = TaskEntity.builder()
            .id(3L)
            .server(server)
            .script(script)
            .status(TaskStatus.SUCCESS)
            .build();
        TaskDto dto = new TaskDto(3L, TaskStatus.SUCCESS, "ok", 1L, 2L, null, null, null);

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(mappingService.mapToDto(task, TaskDto.class)).thenReturn(dto);

        TaskDto result = taskService.getTaskById(3L);

        assertEquals(dto, result);
        verify(statusCache).put(task);
    }

    @Test
    void getAllTasksShouldMapAllOwnedTasks() {
        TaskEntity t1 = TaskEntity.builder().id(1L).status(TaskStatus.PENDING).build();
        TaskEntity t2 = TaskEntity.builder().id(2L).status(TaskStatus.SUCCESS).build();
        List<TaskEntity> entities = List.of(t1, t2);
        List<TaskDto> mapped = List.of(
            new TaskDto(1L, TaskStatus.PENDING, null, null, null, null, null, null),
            new TaskDto(2L, TaskStatus.SUCCESS, null, null, null, null, null, null)
        );

        when(taskRepository.findAllByServerGroupOwnerUsername(TEST_USER)).thenReturn(entities);
        when(mappingService.mapListToDto(entities, TaskDto.class)).thenReturn(mapped);

        List<TaskDto> result = taskService.getAllTasks();

        assertEquals(mapped, result);
    }

    private ScriptEntity scriptOwnedBy(String username, boolean isPublic) {
        return ScriptEntity.builder()
            .id(1L)
            .name("script")
            .content("echo ok")
            .owner(UserEntity.builder().id(1L).username(username).build())
            .isPublic(isPublic)
            .tasks(new ArrayList<>())
            .build();
    }

    private ServerEntity serverOwnedBy(String username) {
        ServerGroupEntity group = ServerGroupEntity.builder()
            .id(10L)
            .name("group")
            .owner(UserEntity.builder().id(7L).username(username).build())
            .servers(Set.of())
            .build();

        return ServerEntity.builder()
            .id(20L)
            .hostname("srv")
            .ipAddress("127.0.0.1")
            .port(22)
            .password("pw")
            .groups(Set.of(group))
            .tasks(new ArrayList<>())
            .build();
    }
}
