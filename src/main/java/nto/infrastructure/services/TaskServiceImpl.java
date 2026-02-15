package nto.infrastructure.services;

import nto.application.dto.ServerDto;
import nto.application.dto.TaskDto;
import nto.application.annotations.LogExecutionTime;
import nto.application.interfaces.mapping.MapperProfile;
import nto.application.interfaces.repositories.TaskRepository;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.TaskService;
import nto.core.entities.TaskEntity;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusCache statusCache;
    private final MappingService mappingService;
    @Override
    @Transactional
    @LogExecutionTime
    public TaskDto createTask(TaskDto dto) {
        //TODO
        // Здесь должен быть код поиска ServerEntity и ScriptEntity из БД

        TaskEntity entity = TaskEntity.builder()
                // ... инициализация полей
                .build();

        TaskEntity saved = taskRepository.save(entity);

        // Инвалидация/Обновление кэша
        statusCache.put(saved);
        return mappingService.mapToDto(saved, TaskDto.class);

    }

    @Override
    public TaskDto getLastStatus(Long serverId, Long scriptId) {
        // Сначала идем в кэш
        TaskEntity cached = statusCache.get(serverId, scriptId);

        if (cached != null) {
            // Map From->To
            return mappingService.mapToDto(cached, TaskDto.class);
        }
        //TODO
        // Если в кэше нет (например, после рестарта), можно пойти в БД (Native query из шага 1)
        // И положить результат в кэш
        return null;
    }
}