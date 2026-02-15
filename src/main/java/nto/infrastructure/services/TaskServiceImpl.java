package nto.infrastructure.services;

import nto.application.dto.TaskDto;
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

    private final JpaTaskRepository taskRepository;
    private final TaskStatusCache statusCache;

    @Override
    @Transactional
    public TaskDto createTask(TaskDto dto) {
        // Здесь должен быть код поиска ServerEntity и ScriptEntity из БД

        TaskEntity entity = TaskEntity.builder()
                // ... инициализация полей
                .build();

        TaskEntity saved = taskRepository.save(entity);

        // Инвалидация/Обновление кэша
        statusCache.put(saved);

        // Map From->To
        return new TaskDto(saved.getId(), saved.getStatus(), saved.getOutput());
    }

    @Override
    public TaskDto getLastStatus(Long serverId, Long scriptId) {
        // Сначала идем в кэш
        TaskEntity cached = statusCache.get(serverId, scriptId);

        if (cached != null) {
            // Map From->To
            return new TaskDto(cached.getId(), cached.getStatus(), cached.getOutput());
        }

        // Если в кэше нет (например, после рестарта), можно пойти в БД (Native query из шага 1)
        // И положить результат в кэш
        return null;
    }
}