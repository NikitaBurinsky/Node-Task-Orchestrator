package nto.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nto.application.annotations.LogExecutionTime;
import nto.application.dto.BulkTaskRequestDto;
import nto.application.dto.TaskDto;
import nto.application.interfaces.repositories.ScriptRepository;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.application.interfaces.services.TaskService;
import nto.core.entities.ScriptEntity;
import nto.core.entities.ServerEntity;
import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import nto.core.utils.exceptions.BadRequestException;
import nto.core.utils.exceptions.ServerBusyException;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final JpaTaskRepository taskRepository;
    private final TaskStatusCache tasksCache;
    private final MappingService mappingService;
    private final ServerRepository serverRepository;
    private final ScriptRepository scriptRepository;
    private final ScriptExecutor scriptExecutor;

    @Value("${nto.executor.type:mock}")
    private String executorType;

    @Override
    @Transactional
    @LogExecutionTime
    public TaskDto createTask(TaskDto dto) {
        if (!"mock".equalsIgnoreCase(executorType)) {
            List<TaskStatus> activeStatuses = List.of(TaskStatus.PENDING, TaskStatus.RUNNING);
            if (taskRepository.existsByServerIdAndStatusIn(dto.serverId(), activeStatuses)) {
                log.warn("Attempt to start task on busy server ID: {}", dto.serverId());
                throw new ServerBusyException(
                    "Сервер уже выполняет другую задачу. Дождитесь завершения.");
            }
        }

        String username = getCurrentUsername();
        ScriptEntity script = getScriptIfAvailable(dto.scriptId(), username);
        ServerEntity server = getServerIfOwned(dto.serverId(), username);
        return createAndSaveTask(script, server);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskDto> getTasksWithFilters(String username, TaskStatus status,
                                             Pageable pageable) {

        Page<TaskEntity> tasks = taskRepository.findTasksByUserAndStatusJPQL(username, status,
            pageable);
        return tasks.map(entity -> mappingService.mapToDto(entity, TaskDto.class));
    }

    @Override
    public TaskDto getLastStatus(Long serverId, Long scriptId) {
        String username = getCurrentUsername();


        getServerIfOwned(serverId, username);
        getScriptIfAvailable(scriptId, username);


        TaskEntity cached = tasksCache.get(serverId, scriptId);
        if (cached != null) {
            return mappingService.mapToDto(cached, TaskDto.class);
        }


        return taskRepository.findFirstByServerIdAndScriptIdOrderByCreatedAtDesc(serverId, scriptId)
            .map(entity -> mappingService.mapToDto(entity, TaskDto.class))
            .orElse(null);
    }

    @Override
    @Transactional
    @LogExecutionTime
    public List<TaskDto> createTasksBulk(BulkTaskRequestDto dto) {
        String username = getCurrentUsername();


        ScriptEntity script = getScriptIfAvailable(dto.scriptId(), username);


        List<ServerEntity> foundServers = serverRepository.findAllById(dto.serverIds());


        validateServersExistence(foundServers, dto.serverIds());


        if (!"mock".equalsIgnoreCase(executorType)) {
            foundServers.forEach(server -> validateServerOwnership(server, username));
        }

        List<TaskEntity> tasksToSave = foundServers.stream()
            .map(server -> buildTask(script, server))
            .collect(Collectors.toList());

        List<TaskEntity> savedTasks = taskRepository.saveAll(tasksToSave);


        savedTasks.forEach(task -> {
            tasksCache.put(task);
            scriptExecutor.executeAsync(task.getId());
        });

        return mappingService.mapListToDto(savedTasks, TaskDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id) {
        String username = getCurrentUsername();

        TaskEntity task = taskRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));


        validateServerOwnership(task.getServer(), username);
        tasksCache.put(task);
        return mappingService.mapToDto(task, TaskDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks() {
        String username = getCurrentUsername();


        List<TaskEntity> tasks = taskRepository.findAllByServerGroupOwnerUsername(username);

        return mappingService.mapListToDto(tasks, TaskDto.class);
    }


    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ScriptEntity getScriptIfAvailable(Long scriptId, String username) {
        ScriptEntity script = scriptRepository.findById(scriptId)
            .orElseThrow(() -> new EntityNotFoundException("Script not found: " + scriptId));

        boolean isOwner = script.getOwner().getUsername().equals(username);
        boolean isPublic = Boolean.TRUE.equals(script.getIsPublic());

        if (!isOwner && !isPublic) {
            throw new AccessDeniedException(
                "Access Denied: Script is private and does not belong to you.");
        }
        return script;
    }

    private ServerEntity getServerIfOwned(Long serverId, String username) {
        ServerEntity server = serverRepository.findById(serverId)
            .orElseThrow(() -> new EntityNotFoundException("Server not found: " + serverId));

        validateServerOwnership(server, username);
        return server;
    }

    private void validateServerOwnership(ServerEntity server, String username) {
        boolean owned = server.getGroups().stream()
            .anyMatch(group -> group.getOwner() != null
                && username.equals(group.getOwner().getUsername()));
        if (!owned) {
            throw new AccessDeniedException(
                "Access Denied: Server " + server.getHostname() + " (ID: " + server.getId() +
                    ") does not belong to you."
            );
        }
    }

    private void validateServersExistence(List<ServerEntity> foundServers,
                                          List<Long> requestedIds) {
        if (foundServers.size() != requestedIds.size()) {
            Set<Long> foundIds = foundServers.stream()
                .map(ServerEntity::getId)
                .collect(Collectors.toSet());

            List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

            throw new BadRequestException("Servers not found: " + missingIds);
        }
    }

    private TaskDto createAndSaveTask(ScriptEntity script, ServerEntity server) {
        TaskEntity entity = buildTask(script, server);
        TaskEntity saved = taskRepository.save(entity);

        tasksCache.put(saved);
        scriptExecutor.executeAsync(saved.getId());

        return mappingService.mapToDto(saved, TaskDto.class);
    }

    private TaskEntity buildTask(ScriptEntity script, ServerEntity server) {
        return TaskEntity.builder()
            .script(script)
            .server(server)
            .status(TaskStatus.PENDING)
            .build();
    }
}
