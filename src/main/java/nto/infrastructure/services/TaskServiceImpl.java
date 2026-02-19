package nto.infrastructure.services;

import lombok.RequiredArgsConstructor;
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
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final JpaTaskRepository taskRepository;
    private final TaskStatusCache statusCache;
    private final MappingService mappingService;
    private final ServerRepository serverRepository;
    private final ScriptRepository scriptRepository;
    private final ScriptExecutor scriptExecutor;

    @Override
    @Transactional
    @LogExecutionTime
    public TaskDto createTask(TaskDto dto) {
        String username = getCurrentUsername();

        ScriptEntity script = getScriptIfAvailable(dto.scriptId(), username);
        ServerEntity server = getServerIfOwned(dto.serverId(), username);

        return createAndSaveTask(script, server);
    }

    @Override
    public TaskDto getLastStatus(Long serverId, Long scriptId) {
        String username = getCurrentUsername();

        // Проверяем права доступа (Fail-fast)
        getServerIfOwned(serverId, username);
        getScriptIfAvailable(scriptId, username);

        // 1. Hot Data (Cache)
        TaskEntity cached = statusCache.get(serverId, scriptId);
        if (cached != null) {
            return mappingService.mapToDto(cached, TaskDto.class);
        }

        // 2. Cache Miss -> DB Hit (Закрыли TODO)
        // Ищем последнюю запись в БД. Если нет - возвращаем null (или можно кидать 404, зависит от фронта)
        return taskRepository.findFirstByServerIdAndScriptIdOrderByCreatedAtDesc(serverId, scriptId)
            .map(entity -> mappingService.mapToDto(entity, TaskDto.class))
            .orElse(null);
    }

    @Override
    @Transactional
    @LogExecutionTime
    public List<TaskDto> createTasksBulk(BulkTaskRequestDto dto) {
        String username = getCurrentUsername();

        // 1. Валидация скрипта
        ScriptEntity script = getScriptIfAvailable(dto.scriptId(), username);

        // 2. Пакетная загрузка серверов
        List<ServerEntity> foundServers = serverRepository.findAllById(dto.serverIds());

        // 3. Валидация целостности списка (все ли ID найдены в базе)
        validateServersExistence(foundServers, dto.serverIds());

        // 4. Валидация прав (все ли серверы принадлежат юзеру)
        foundServers.forEach(server -> validateServerOwnership(server, username));

        // 5. Создание и сохранение задач
        List<TaskEntity> tasksToSave = foundServers.stream()
            .map(server -> buildTask(script, server))
            .collect(Collectors.toList());

        List<TaskEntity> savedTasks = taskRepository.saveAll(tasksToSave);

        // 6. Сайд-эффекты (Кэш + Async Executor)
        savedTasks.forEach(task -> {
            statusCache.put(task);
            scriptExecutor.executeAsync(task.getId());
        });

        return mappingService.mapListToDto(savedTasks, TaskDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id) {
        String username = getCurrentUsername();

        TaskEntity task = taskRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        // Security Check: Пользователь может видеть задачу только если он владелец сервера, на котором она выполнялась.
        if (!task.getServer().getOwner().getUsername().equals(username)) {
            throw new RuntimeException(
                "Access Denied: Task does not belong to your server context.");
        }

        return mappingService.mapToDto(task, TaskDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks() {
        String username = getCurrentUsername();

        // Security Check: Возвращаем только задачи с серверов текущего пользователя
        List<TaskEntity> tasks = taskRepository.findAllByServerOwnerUsername(username);

        return mappingService.mapListToDto(tasks, TaskDto.class);
    }

    // Private Helper Methods (DRY & Security)

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * Получает скрипт и проверяет права доступа (Владелец или Публичный).
     */
    private ScriptEntity getScriptIfAvailable(Long scriptId, String username) {
        ScriptEntity script = scriptRepository.findById(scriptId)
            .orElseThrow(() -> new RuntimeException("Script not found: " + scriptId));

        boolean isOwner = script.getOwner().getUsername().equals(username);
        boolean isPublic = Boolean.TRUE.equals(script.getIsPublic());

        if (!isOwner && !isPublic) {
            throw new RuntimeException(
                "Access Denied: Script is private and does not belong to you.");
        }
        return script;
    }

    /**
     * Получает сервер и проверяет права владельца.
     */
    private ServerEntity getServerIfOwned(Long serverId, String username) {
        ServerEntity server = serverRepository.findById(serverId)
            .orElseThrow(() -> new RuntimeException("Server not found: " + serverId));

        validateServerOwnership(server, username);
        return server;
    }

    /**
     * Проверяет, что сервер принадлежит пользователю.
     */
    private void validateServerOwnership(ServerEntity server, String username) {
        if (!server.getOwner().getUsername().equals(username)) {
            throw new RuntimeException(
                "Access Denied: Server " + server.getHostname() + " (ID: " + server.getId() +
                    ") does not belong to you."
            );
        }
    }

    /**
     * Проверяет, что все запрошенные ID серверов были найдены в БД.
     */
    private void validateServersExistence(List<ServerEntity> foundServers,
                                          List<Long> requestedIds) {
        if (foundServers.size() != requestedIds.size()) {
            Set<Long> foundIds = foundServers.stream()
                .map(ServerEntity::getId)
                .collect(Collectors.toSet());

            List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
            throw new RuntimeException("Rollback! Servers not found: " + missingIds);
        }
    }

    private TaskDto createAndSaveTask(ScriptEntity script, ServerEntity server) {
        TaskEntity entity = buildTask(script, server);
        TaskEntity saved = taskRepository.save(entity);

        statusCache.put(saved);
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