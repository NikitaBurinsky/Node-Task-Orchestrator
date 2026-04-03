package nto.services;

import jakarta.persistence.EntityNotFoundException;
import nto.application.dto.BulkCreateServersGroupRequestDto;
import nto.application.dto.ServerDto;
import nto.application.dto.ServerGroupDto;
import nto.application.dto.TaskDto;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.application.interfaces.services.ServerService;
import nto.core.entities.ScriptEntity;
import nto.core.entities.ServerEntity;
import nto.core.entities.ServerGroupEntity;
import nto.core.entities.TaskEntity;
import nto.core.entities.UserEntity;
import nto.core.enums.TaskStatus;
import nto.core.utils.ServerGroupDefaults;
import nto.core.utils.exceptions.BadRequestException;
import nto.core.utils.exceptions.ResourceConflictException;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaScriptRepository;
import nto.infrastructure.repositories.JpaServerGroupRepository;
import nto.infrastructure.repositories.JpaServerRepository;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.repositories.JpaUserRepository;
import nto.infrastructure.services.ServerGroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerGroupServiceImplTest {

    private static final String TEST_USERNAME = "admin";

    @Mock
    private JpaServerGroupRepository groupRepository;
    @Mock
    private JpaServerRepository serverRepository;
    @Mock
    private JpaUserRepository userRepository;
    @Mock
    private JpaScriptRepository scriptRepository;
    @Mock
    private JpaTaskRepository taskRepository;
    @Mock
    private MappingService mappingService;
    @Mock
    private ScriptExecutor scriptExecutor;
    @Mock
    private ServerService serverService;
    @Mock
    private TaskStatusCache statusCache;
    @InjectMocks
    private ServerGroupServiceImpl groupService;

    @BeforeEach
    void setUpSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(TEST_USERNAME);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createGroupShouldPersistMappedEntityWithCurrentOwner() {
        UserEntity owner = user(TEST_USERNAME);
        ServerGroupDto input = new ServerGroupDto(null, "ops", List.of());
        ServerGroupEntity mappedEntity = group("ops", owner);
        ServerGroupEntity savedEntity = group("ops", owner);
        savedEntity.setId(10L);
        ServerGroupDto expected = new ServerGroupDto(10L, "ops", List.of());

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(owner));
        when(mappingService.mapToEntity(input, ServerGroupEntity.class)).thenReturn(mappedEntity);
        when(groupRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(mappingService.mapToDto(savedEntity, ServerGroupDto.class)).thenReturn(expected);

        ServerGroupDto result = groupService.createGroup(input);

        assertEquals(expected, result);
        assertEquals(owner, mappedEntity.getOwner());
    }

    @Test
    void createGroupShouldThrowWhenCurrentUserNotFound() {
        ServerGroupDto input = new ServerGroupDto(null, "ops", List.of());
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> groupService.createGroup(input));
    }

    @Test
    void getGroupByIdShouldThrowWhenGroupOwnedByAnotherUser() {
        ServerGroupEntity foreignGroup = group("ops", user("another"));
        foreignGroup.setId(10L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(foreignGroup));

        assertThrows(AccessDeniedException.class, () -> groupService.getGroupById(10L));
    }

    @Test
    void getGroupByIdShouldReturnMappedDtoForOwner() {
        ServerGroupEntity owned = group("ops", user(TEST_USERNAME));
        owned.setId(15L);
        ServerGroupDto expected = new ServerGroupDto(15L, "ops", List.of());

        when(groupRepository.findById(15L)).thenReturn(Optional.of(owned));
        when(mappingService.mapToDto(owned, ServerGroupDto.class)).thenReturn(expected);

        ServerGroupDto result = groupService.getGroupById(15L);

        assertEquals(expected, result);
    }

    @Test
    void getAllGroupsShouldMapRepositoryResult() {
        List<ServerGroupEntity> entities = List.of(
            group("g1", user(TEST_USERNAME)),
            group("g2", user(TEST_USERNAME))
        );
        List<ServerGroupDto> expected = List.of(
            new ServerGroupDto(1L, "g1", List.of()),
            new ServerGroupDto(2L, "g2", List.of())
        );

        when(groupRepository.findAllByOwnerUsername(TEST_USERNAME)).thenReturn(entities);
        when(mappingService.mapListToDto(entities, ServerGroupDto.class)).thenReturn(expected);

        List<ServerGroupDto> result = groupService.getAllGroups();

        assertEquals(expected, result);
    }

    @Test
    void deleteGroupShouldDeleteNonDefaultGroup() {
        ServerGroupEntity group = group("custom", user(TEST_USERNAME));
        group.setId(3L);

        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));

        groupService.deleteGroup(3L);

        verify(groupRepository).delete(group);
    }

    @Test
    void deleteGroupShouldThrowForDefaultGroup() {
        ServerGroupEntity group = group(ServerGroupDefaults.DEFAULT_GROUP_NAME, user(TEST_USERNAME));
        group.setId(3L);

        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));

        assertThrows(ResourceConflictException.class, () -> groupService.deleteGroup(3L));
        verify(groupRepository, never()).delete(any(ServerGroupEntity.class));
    }

    @Test
    void addServerToGroupShouldAttachAndPersistServer() {
        ServerGroupEntity group = group("ops", user(TEST_USERNAME));
        group.setId(100L);
        ServerEntity server = serverOwnedBy(TEST_USERNAME);
        server.setId(200L);

        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(serverRepository.findById(200L)).thenReturn(Optional.of(server));

        groupService.addServerToGroup(100L, 200L);

        assertTrue(server.getGroups().contains(group));
        assertTrue(group.getServers().contains(server));
        verify(serverRepository).save(server);
    }

    @Test
    void removeServerFromGroupShouldDetachAndPersistServer() {
        ServerGroupEntity group = group("ops", user(TEST_USERNAME));
        group.setId(100L);
        ServerEntity server = serverOwnedBy(TEST_USERNAME);
        server.setId(200L);
        server.getGroups().add(group);
        group.getServers().add(server);

        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(serverRepository.findById(200L)).thenReturn(Optional.of(server));

        groupService.removeServerFromGroup(100L, 200L);

        assertFalse(server.getGroups().contains(group));
        assertFalse(group.getServers().contains(server));
        verify(serverRepository).save(server);
    }

    @Test
    void removeServerFromGroupShouldThrowForDefaultGroup() {
        ServerGroupEntity group = group(ServerGroupDefaults.DEFAULT_GROUP_NAME, user(TEST_USERNAME));
        group.setId(100L);

        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));

        assertThrows(ResourceConflictException.class, () -> groupService.removeServerFromGroup(100L, 200L));
        verify(serverRepository, never()).findById(any());
    }

    @Test
    void pingGroupShouldReturnMapOfStatuses() {
        Long groupId = 1L;

        UserEntity owner = user(TEST_USERNAME);

        ServerEntity server1 = new ServerEntity();
        server1.setId(10L);
        ServerEntity server2 = new ServerEntity();
        server2.setId(20L);

        ServerGroupEntity group = new ServerGroupEntity();
        group.setId(groupId);
        group.setOwner(owner);
        group.setServers(new HashSet<>(List.of(server1, server2)));

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(scriptExecutor.ping(10L)).thenReturn(true);
        when(scriptExecutor.ping(20L)).thenReturn(false);

        Map<Long, Boolean> results = groupService.pingGroup(groupId);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.get(10L));
        assertFalse(results.get(20L));
    }

    @Test
    void createGroupWithServersBulkShouldCreateGroupAndAttachServers() {
        UserEntity owner = user(TEST_USERNAME);

        ServerDto firstServerRequest = new ServerDto(null, "srv-1", "10.0.0.1", 22, "root", "pw1");
        ServerDto secondServerRequest = new ServerDto(null, "srv-2", "10.0.0.2", 22, "root", "pw2");
        BulkCreateServersGroupRequestDto requestDto = new BulkCreateServersGroupRequestDto(
            "bulk-group", List.of(firstServerRequest, secondServerRequest)
        );

        ServerDto firstCreatedServer = new ServerDto(10L, "srv-1", "10.0.0.1", 22, "root", "pw1");
        ServerDto secondCreatedServer = new ServerDto(20L, "srv-2", "10.0.0.2", 22, "root", "pw2");

        ServerEntity firstServerEntity = serverOwnedBy(TEST_USERNAME);
        firstServerEntity.setId(10L);
        ServerEntity secondServerEntity = serverOwnedBy(TEST_USERNAME);
        secondServerEntity.setId(20L);

        ServerGroupEntity savedGroup = group("bulk-group", owner);
        savedGroup.setId(100L);

        ServerGroupDto expectedResult = new ServerGroupDto(
            100L, "bulk-group", List.of(firstCreatedServer, secondCreatedServer)
        );

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(owner));
        when(groupRepository.findByOwnerUsernameAndName(TEST_USERNAME, "bulk-group"))
            .thenReturn(Optional.empty());
        when(groupRepository.save(any(ServerGroupEntity.class))).thenReturn(savedGroup);
        when(serverService.createServer(firstServerRequest)).thenReturn(firstCreatedServer);
        when(serverService.createServer(secondServerRequest)).thenReturn(secondCreatedServer);
        when(serverRepository.findById(10L)).thenReturn(Optional.of(firstServerEntity));
        when(serverRepository.findById(20L)).thenReturn(Optional.of(secondServerEntity));
        when(serverRepository.save(any(ServerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(savedGroup));
        when(mappingService.mapToDto(savedGroup, ServerGroupDto.class)).thenReturn(expectedResult);

        ServerGroupDto result = groupService.createGroupWithServersBulk(requestDto);

        assertEquals(expectedResult, result);
        assertTrue(savedGroup.getServers().contains(firstServerEntity));
        assertTrue(savedGroup.getServers().contains(secondServerEntity));
        verify(serverService).createServer(firstServerRequest);
        verify(serverService).createServer(secondServerRequest);
        verify(serverRepository, times(2)).save(any(ServerEntity.class));
    }

    @Test
    void createGroupWithServersBulkShouldThrowConflictWhenGroupNameExists() {
        UserEntity owner = user(TEST_USERNAME);

        BulkCreateServersGroupRequestDto requestDto = new BulkCreateServersGroupRequestDto(
            "bulk-group", List.of(new ServerDto(null, "srv-1", "10.0.0.1", 22, "root", "pw1"))
        );

        ServerGroupEntity existingGroup = group("bulk-group", owner);
        existingGroup.setId(5L);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(owner));
        when(groupRepository.findByOwnerUsernameAndName(TEST_USERNAME, "bulk-group"))
            .thenReturn(Optional.of(existingGroup));

        assertThrows(ResourceConflictException.class,
            () -> groupService.createGroupWithServersBulk(requestDto));

        verify(groupRepository, never()).save(any(ServerGroupEntity.class));
        verify(serverService, never()).createServer(any(ServerDto.class));
    }

    @Test
    void createGroupWithServersBulkShouldPropagateExceptionFromServerCreation() {
        UserEntity owner = user(TEST_USERNAME);

        ServerDto firstServerRequest = new ServerDto(null, "srv-1", "10.0.0.1", 22, "root", "pw1");
        ServerDto secondServerRequest = new ServerDto(null, "srv-2", "10.0.0.2", 22, "root", "pw2");
        BulkCreateServersGroupRequestDto requestDto = new BulkCreateServersGroupRequestDto(
            "bulk-group", List.of(firstServerRequest, secondServerRequest)
        );

        ServerGroupEntity savedGroup = group("bulk-group", owner);
        savedGroup.setId(100L);

        ServerDto firstCreatedServer = new ServerDto(10L, "srv-1", "10.0.0.1", 22, "root", "pw1");
        ServerEntity firstServerEntity = serverOwnedBy(TEST_USERNAME);
        firstServerEntity.setId(10L);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(owner));
        when(groupRepository.findByOwnerUsernameAndName(TEST_USERNAME, "bulk-group"))
            .thenReturn(Optional.empty());
        when(groupRepository.save(any(ServerGroupEntity.class))).thenReturn(savedGroup);
        when(serverService.createServer(firstServerRequest)).thenReturn(firstCreatedServer);
        when(serverService.createServer(secondServerRequest))
            .thenThrow(new BadRequestException("Invalid server payload"));
        when(serverRepository.findById(10L)).thenReturn(Optional.of(firstServerEntity));
        when(serverRepository.save(any(ServerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BadRequestException.class,
            () -> groupService.createGroupWithServersBulk(requestDto));

        verify(serverRepository, times(1)).save(any(ServerEntity.class));
        verify(groupRepository, never()).findById(100L);
        verify(mappingService, never()).mapToDto(any(ServerGroupEntity.class), eq(ServerGroupDto.class));
    }

    @Test
    void executeScriptOnGroupShouldThrowWhenScriptIsNotFound() {
        ServerGroupEntity group = group("ops", user(TEST_USERNAME));
        group.setId(500L);

        when(groupRepository.findById(500L)).thenReturn(Optional.of(group));
        when(scriptRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> groupService.executeScriptOnGroup(500L, 99L));
    }

    @Test
    void executeScriptOnGroupShouldThrowForPrivateScriptOwnedByAnotherUser() {
        ServerGroupEntity group = group("ops", user(TEST_USERNAME));
        group.setId(500L);
        group.getServers().add(serverOwnedBy(TEST_USERNAME));

        ScriptEntity script = ScriptEntity.builder()
            .id(99L)
            .name("script")
            .content("echo")
            .isPublic(false)
            .owner(user("other"))
            .build();

        when(groupRepository.findById(500L)).thenReturn(Optional.of(group));
        when(scriptRepository.findById(99L)).thenReturn(Optional.of(script));

        assertThrows(AccessDeniedException.class, () -> groupService.executeScriptOnGroup(500L, 99L));
    }

    @Test
    void executeScriptOnGroupShouldThrowWhenGroupHasNoServers() {
        ServerGroupEntity group = group("ops", user(TEST_USERNAME));
        group.setId(500L);

        ScriptEntity script = ScriptEntity.builder()
            .id(99L)
            .name("script")
            .content("echo")
            .isPublic(true)
            .owner(user("other"))
            .build();

        when(groupRepository.findById(500L)).thenReturn(Optional.of(group));
        when(scriptRepository.findById(99L)).thenReturn(Optional.of(script));

        assertThrows(ResourceConflictException.class, () -> groupService.executeScriptOnGroup(500L, 99L));
    }

    @Test
    void executeScriptOnGroupShouldCreateTasksAndTriggerAsyncExecution() {
        ServerGroupEntity group = group("ops", user(TEST_USERNAME));
        group.setId(500L);
        ServerEntity s1 = serverOwnedBy(TEST_USERNAME);
        s1.setId(1L);
        ServerEntity s2 = serverOwnedBy(TEST_USERNAME);
        s2.setId(2L);
        group.getServers().addAll(List.of(s1, s2));

        ScriptEntity script = ScriptEntity.builder()
            .id(99L)
            .name("script")
            .content("echo")
            .isPublic(false)
            .owner(user(TEST_USERNAME))
            .build();

        TaskEntity t1 = TaskEntity.builder()
            .id(10L)
            .server(s1)
            .script(script)
            .sourceGroup(group)
            .status(TaskStatus.PENDING)
            .build();
        TaskEntity t2 = TaskEntity.builder()
            .id(11L)
            .server(s2)
            .script(script)
            .sourceGroup(group)
            .status(TaskStatus.PENDING)
            .build();

        List<TaskDto> expected = List.of(
            new TaskDto(10L, TaskStatus.PENDING, null, 1L, 99L, 500L, null, null),
            new TaskDto(11L, TaskStatus.PENDING, null, 2L, 99L, 500L, null, null)
        );

        when(groupRepository.findById(500L)).thenReturn(Optional.of(group));
        when(scriptRepository.findById(99L)).thenReturn(Optional.of(script));
        when(taskRepository.saveAll(any())).thenReturn(List.of(t1, t2));
        when(mappingService.mapListToDto(List.of(t1, t2), TaskDto.class)).thenReturn(expected);

        List<TaskDto> result = groupService.executeScriptOnGroup(500L, 99L);

        assertEquals(expected, result);
        verify(taskRepository, times(2)).saveAll(any());
        verify(statusCache).put(t1);
        verify(statusCache).put(t2);
        verify(scriptExecutor).executeAsync(10L);
        verify(scriptExecutor).executeAsync(11L);

        ArgumentCaptor<List<TaskEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository, times(2)).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void getLastGroupExecutionStatusShouldReadRepositoryAndMap() {
        ServerGroupEntity group = group("ops", user(TEST_USERNAME));
        group.setId(77L);
        TaskEntity task = TaskEntity.builder().id(1L).status(TaskStatus.SUCCESS).build();
        List<TaskDto> expected = List.of(
            new TaskDto(1L, TaskStatus.SUCCESS, "ok", null, null, null, null, null)
        );

        when(groupRepository.findById(77L)).thenReturn(Optional.of(group));
        when(taskRepository.findLatestTasksByGroupId(77L)).thenReturn(List.of(task));
        when(mappingService.mapListToDto(List.of(task), TaskDto.class)).thenReturn(expected);

        List<TaskDto> result = groupService.getLastGroupExecutionStatus(77L);

        assertEquals(expected, result);
    }

    private UserEntity user(String username) {
        return UserEntity.builder().id(1L).username(username).build();
    }

    private ServerGroupEntity group(String name, UserEntity owner) {
        return ServerGroupEntity.builder()
            .name(name)
            .owner(owner)
            .servers(new HashSet<>())
            .build();
    }

    private ServerEntity serverOwnedBy(String username) {
        ServerGroupEntity ownerGroup = group("owner", user(username));
        ServerEntity entity = ServerEntity.builder()
            .hostname("srv")
            .ipAddress("10.0.0.1")
            .port(22)
            .password("pw")
            .groups(new HashSet<>())
            .build();
        entity.getGroups().add(ownerGroup);
        return entity;
    }
}
