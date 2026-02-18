package nto.infrastructure.services;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerGroupDto;
import nto.application.dto.TaskDto;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.application.interfaces.services.ServerGroupService;
import nto.core.entities.*;
import nto.core.enums.TaskStatus;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaScriptRepository;
import nto.infrastructure.repositories.JpaServerGroupRepository;
import nto.infrastructure.repositories.JpaServerRepository;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.repositories.JpaUserRepository;
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
    private final TaskStatusCache statusCache;

    @Override
    @Transactional
    public ServerGroupDto createGroup(ServerGroupDto dto) {
        String username = getCurrentUsername();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ServerGroupEntity entity = mappingService.mapToEntity(dto, ServerGroupEntity.class);
        entity.setOwner(user);

        return mappingService.mapToDto(groupRepository.save(entity), ServerGroupDto.class);
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
        // Разрываем связи с серверами перед удалением, чтобы не посыпался Cascade (если настроен жестко)
        // Но так как у нас ManyToMany владелец Server, hibernate сам почистит link table
        groupRepository.delete(group);
    }

    @Override
    @Transactional
    public void addServerToGroup(Long groupId, Long serverId) {
        ServerGroupEntity group = getGroupIfOwned(groupId);
        ServerEntity server = getServerIfOwned(serverId);

        // ManyToMany связь, обновляем обе стороны для консистентности в кэше hibernate
        server.getGroups().add(group);
        group.getServers().add(server);

        serverRepository.save(server); // Server владелец связи
    }

    @Override
    @Transactional
    public void removeServerFromGroup(Long groupId, Long serverId) {
        ServerGroupEntity group = getGroupIfOwned(groupId);
        ServerEntity server = getServerIfOwned(serverId);

        server.getGroups().remove(group);
        group.getServers().remove(server);

        serverRepository.save(server);
    }

    @Override
    public Map<Long, Boolean> pingGroup(Long groupId) {
        ServerGroupEntity group = getGroupIfOwned(groupId);
        Map<Long, Boolean> results = new HashMap<>();

        // В реальном проекте это лучше делать параллельно (Parallel Stream или CompletableFuture)
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
                .orElseThrow(() -> new RuntimeException("Script not found"));

        if (!script.getIsPublic() && !script.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("Access Denied to Script");
        }

        if (group.getServers().isEmpty()) {
            throw new RuntimeException("Group is empty");
        }

        // Создаем задачи
        List<TaskEntity> tasks = group.getServers().stream()
                .map(server -> TaskEntity.builder()
                        .server(server)
                        .script(script)
                        .sourceGroup(group)
                        .status(TaskStatus.PENDING)
                        .build())
                .collect(Collectors.toList());

        List<TaskEntity> savedTasks = taskRepository.saveAll(tasks);

        // Асинхронный запуск
        savedTasks.forEach(task -> {
            statusCache.put(task);
            scriptExecutor.executeAsync(task.getId());
        });

        return mappingService.mapListToDto(savedTasks, TaskDto.class);
    }

    // --- Private Helpers ---

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ServerGroupEntity getGroupIfOwned(Long id) {
        ServerGroupEntity group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        if (!group.getOwner().getUsername().equals(getCurrentUsername())) {
            throw new RuntimeException("Access Denied: Not your group");
        }
        return group;
    }

    private ServerEntity getServerIfOwned(Long id) {
        ServerEntity server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found"));
        if (!server.getOwner().getUsername().equals(getCurrentUsername())) {
            throw new RuntimeException("Access Denied: Not your server");
        }
        return server;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getLastGroupExecutionStatus(Long groupId) {
        // 1. Проверяем доступ к группе (Security)
        getGroupIfOwned(groupId);

        // 2. Ищем последние задачи через умный запрос в репозитории
        List<TaskEntity> tasks = taskRepository.findLatestTasksByGroupId(groupId);

        return mappingService.mapListToDto(tasks, TaskDto.class);
    }
}