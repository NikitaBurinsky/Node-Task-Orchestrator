package nto.infrastructure.cache;

import nto.core.entities.TaskEntity;
import nto.core.enums.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskStatusCache {
    // Ключ: "serverId_scriptId"
    private final Map<String, TaskEntity> cache = new ConcurrentHashMap<>();
    private final Map<Long, TaskEntity> taskscache = new ConcurrentHashMap<>();

    public void put(TaskEntity task) {
        String key = buildKey(task.getServer().getId(), task.getScript().getId());
        cache.put(key, task);
        taskscache.put(task.getId(), task);
    }

    public TaskEntity get(Long serverId, Long scriptId) {
        return cache.get(buildKey(serverId, scriptId));
    }

    public TaskEntity get(Long TaskId){
        return taskscache.get(TaskId);
    }

    public void evict(Long serverId, Long scriptId) {
        Long id = cache.remove(buildKey(serverId, scriptId)).getId();
        taskscache.remove(id);
    }

    private String buildKey(Long serverId, Long scriptId) {
        return serverId + "_" + scriptId;
    }
}