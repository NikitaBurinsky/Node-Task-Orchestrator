package nto.infrastructure.services;

import lombok.RequiredArgsConstructor;
import nto.application.annotations.LogExecutionTime;
import nto.application.dto.BulkTaskRequestDto; // Исправлен импорт
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
    private final ScriptExecutor scriptExecutor; // <--- Inject
    @Override
    @Transactional
    @LogExecutionTime // Применяем аспект (Лаба 4)
    public TaskDto createTask(TaskDto dto) {
        TaskEntity entity = mappingService.mapToEntity(dto, TaskEntity.class);

        // 2. Save
        TaskEntity saved = taskRepository.save(entity);

        // 3. Cache Update
        statusCache.put(saved);

        scriptExecutor.executeAsync(saved.getId());
        // 4. Map Entity -> Dto
        return mappingService.mapToDto(saved, TaskDto.class);
    }

    @Override
    public TaskDto getLastStatus(Long serverId, Long scriptId) {
        // 1. Cache Hit
        TaskEntity cached = statusCache.get(serverId, scriptId);

        if (cached != null) {
            return mappingService.mapToDto(cached, TaskDto.class);
        }

        // 2. Cache Miss -> DB Hit (Native Query из Лабы 3)
        // Логика: если нет в кэше, ищем последнюю запись в БД
        // (Этот блок можно дореализовать, если требуется по заданию, пока вернем null)
        return null;
    }
    @Override
    @Transactional // <--- Гарантирует атомарность. Любой RuntimeException вызовет Rollback.
    @LogExecutionTime // Замеряем время выполнения всей пачки
    public List<TaskDto> createTasksBulk(BulkTaskRequestDto dto) {
        // 1. Загружаем скрипт (один раз)
        ScriptEntity script = scriptRepository.findById(dto.scriptId())
                .orElseThrow(() -> new RuntimeException("Script not found: " + dto.scriptId()));

        // 2. Пакетная загрузка серверов (решает проблему N+1 запросов)
        // findAllById вернет только те, что нашел
        List<ServerEntity> foundServers = serverRepository.findAllById(dto.serverIds());

        // 3. Валидация целостности
        if (foundServers.size() != dto.serverIds().size()) {
            // Вычисляем, каких ID не хватает, для информативной ошибки
            Set<Long> foundIds = foundServers.stream()
                    .map(ServerEntity::getId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = dto.serverIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            // Выброс исключения откатит транзакцию! Ни одна задача не создастся.
            throw new RuntimeException("Rollback! Servers not found: " + missingIds);
        }

        // 4. In-Memory создание сущностей
        List<TaskEntity> tasksToSave = foundServers.stream()
                .map(server -> TaskEntity.builder()
                        .script(script)
                        .server(server)
                        .status(TaskStatus.PENDING)
                        .build())
                .collect(Collectors.toList());

        // 5. Batch Save (Hibernate попытается сделать batch insert)
        List<TaskEntity> savedTasks = taskRepository.saveAll(tasksToSave);

        // 6. Обновление кэша
        savedTasks.forEach(statusCache::put);
        savedTasks.forEach(t -> scriptExecutor.executeAsync(t.getId()));
        // 7. Маппинг результата
        return mappingService.mapListToDto(savedTasks, TaskDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
        return mappingService.mapToDto(task, TaskDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks() {
        //TODO
        // пагинация
        List<TaskEntity> tasks = taskRepository.findAll();

        return mappingService.mapListToDto(tasks, TaskDto.class);
    }
}