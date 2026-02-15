package nto.infrastructure.cache;

import nto.core.entities.TaskEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskStatusCache {
    // Ключ: "serverId_scriptId"
    private final Map<String, TaskEntity> cache = new ConcurrentHashMap<>();

    public void put(TaskEntity task) {
        String key = buildKey(task.getServer().getId(), task.getScript().getId());
        cache.put(key, task);
    }

    public TaskEntity get(Long serverId, Long scriptId) {
        return cache.get(buildKey(serverId, scriptId));
    }

    public void evict(Long serverId, Long scriptId) {
        cache.remove(buildKey(serverId, scriptId));
    }

    private String buildKey(Long serverId, Long scriptId) {
        return serverId + "_" + scriptId;
    }
}