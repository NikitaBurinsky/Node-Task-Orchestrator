package nto.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nto.application.dto.BulkCreateServersGroupRequestDto;
import nto.application.dto.ServerGroupDto;
import nto.application.dto.ServerDto;
import nto.application.dto.TaskDto;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.application.interfaces.services.ServerGroupService;
import nto.application.interfaces.services.ServerService;
import nto.core.entities.ScriptEntity;
import nto.core.entities.ServerEntity;
import nto.core.entities.ServerGroupEntity;
import nto.core.entities.TaskEntity;
import nto.core.entities.UserEntity;
import nto.core.enums.TaskStatus;
import nto.core.utils.ErrorMessages;
import nto.core.utils.ServerGroupDefaults;
import nto.core.utils.exceptions.ResourceConflictException;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaScriptRepository;
import nto.infrastructure.repositories.JpaServerGroupRepository;
import nto.infrastructure.repositories.JpaServerRepository;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.repositories.JpaUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServerGroupServiceImpl implements ServerGroupService {

    private final JpaServerGroupRepository groupRepository;
    private final JpaServerRepository serverRepository;
    private final JpaUserRepository userRepository;
    private final JpaScriptRepository scriptRepository;
    private final JpaTaskRepository taskRepository;

    private final MappingService mappingService;
    private final ScriptExecutor scriptExecutor;
    private final ServerService serverService;
    private final TaskStatusCache statusCache;

    @Override
    @Transactional
    public ServerGroupDto createGroup(ServerGroupDto dto) {
        String username = getCurrentUsername();
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(
                () -> new EntityNotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage()));

        ServerGroupEntity entity = mappingService.mapToEntity(dto, ServerGroupEntity.class);
        entity.setOwner(user);

        return mappingService.mapToDto(groupRepository.save(entity), ServerGroupDto.class);
    }

    @Override
    public ServerGroupDto createGroupWithServersBulk(BulkCreateServersGroupRequestDto dto) {
        String username = getCurrentUsername();
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(
                () -> new EntityNotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage()));

        groupRepository.findByOwnerUsernameAndName(username, dto.name())
            .ifPresent(group -> {
                throw new ResourceConflictException("Group with this name already exists");
            });

        ServerGroupEntity group = groupRepository.save(ServerGroupEntity.builder()
            .name(dto.name())
            .owner(user)
            .build());

        for (ServerDto serverDto : dto.servers()) {
            ServerDto createdServer = serverService.createServer(serverDto);
            ServerEntity server = serverRepository.findById(createdServer.id())
                .orElseThrow(
                    () -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));

            server.getGroups().add(group);
            group.getServers().add(server);
            serverRepository.save(server);
        }

        return mappingService.mapToDto(getGroupIfOwned(group.getId()), ServerGroupDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ServerGroupDto getGroupById(Long id) {
        return mappingService.mapToDto(getGroupIfOwned(id), ServerGroupDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServerGroupDto> getAllGroups() {
        String username = getCurrentUsername();
        return mappingService.mapListToDto(
            groupRepository.findAllByOwnerUsername(username),
            ServerGroupDto.class
        );
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        ServerGroupEntity group = getGroupIfOwned(id);
        if (ServerGroupDefaults.DEFAULT_GROUP_NAME.equals(group.getName())) {
            throw new ResourceConflictException("Default group cannot be deleted");
        }


        groupRepository.delete(group);
    }

    @Override
    @Transactional
    public void addServerToGroup(Long groupId, Long serverId) {
        ServerGroupEntity group = getGroupIfOwned(groupId);
        ServerEntity server = getServerIfOwned(serverId);


        server.getGroups().add(group);
        group.getServers().add(server);

        serverRepository.save(server);
    }

    @Override
    @Transactional
    public void removeServerFromGroup(Long groupId, Long serverId) {
        ServerGroupEntity group = getGroupIfOwned(groupId);
        if (ServerGroupDefaults.DEFAULT_GROUP_NAME.equals(group.getName())) {
            throw new ResourceConflictException("Cannot remove server from default group");
        }
        ServerEntity server = getServerIfOwned(serverId);

        server.getGroups().remove(group);
        group.getServers().remove(server);

        serverRepository.save(server);
    }

    @Override
    public Map<Long, Boolean> pingGroup(Long groupId) {
        ServerGroupEntity group = getGroupIfOwned(groupId);
        Map<Long, Boolean> results = new HashMap<>();


        for (ServerEntity server : group.getServers()) {
            boolean alive = scriptExecutor.ping(server.getId());
            results.put(server.getId(), alive);
        }
        return results;
    }

    @Override
    @Transactional
    public List<TaskDto> executeScriptOnGroup(Long groupId, Long scriptId) {
        String username = getCurrentUsername();
        ServerGroupEntity group = getGroupIfOwned(groupId);

        ScriptEntity script = scriptRepository.findById(scriptId)
            .orElseThrow(() -> new EntityNotFoundException("Script not found"));

        if (!script.getIsPublic() && !script.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Access Denied to Script");
        }

        if (group.getServers().isEmpty()) {
            throw new ResourceConflictException("Group is empty");
        }


        List<TaskEntity> tasks = group.getServers().stream()
            .map(server -> TaskEntity.builder()
                .server(server)
                .script(script)
                .sourceGroup(group)
                .status(TaskStatus.PENDING)
                .build())
            .collect(Collectors.toList());

        List<TaskEntity> savedTasks = taskRepository.saveAll(tasks);


        taskRepository.saveAll(savedTasks);

        savedTasks.forEach(task -> {
            statusCache.put(task);
            scriptExecutor.executeAsync(task.getId());
        });

        return mappingService.mapListToDto(savedTasks, TaskDto.class);
    }


    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ServerGroupEntity getGroupIfOwned(Long id) {
        ServerGroupEntity group = groupRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Group not found"));
        if (!group.getOwner().getUsername().equals(getCurrentUsername())) {
            throw new AccessDeniedException(ErrorMessages.ACCESS_DENIED + ": Not your group");
        }
        return group;
    }

    private ServerEntity getServerIfOwned(Long id) {
        ServerEntity server = serverRepository.findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));
        String username = getCurrentUsername();
        boolean owned = server.getGroups().stream()
            .anyMatch(group -> group.getOwner() != null
                && username.equals(group.getOwner().getUsername()));
        if (!owned) {
            throw new AccessDeniedException(ErrorMessages.ACCESS_DENIED + ": Not your server");
        }
        return server;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getLastGroupExecutionStatus(Long groupId) {

        getGroupIfOwned(groupId);


        List<TaskEntity> tasks = taskRepository.findLatestTasksByGroupId(groupId);

        return mappingService.mapListToDto(tasks, TaskDto.class);
    }
}
