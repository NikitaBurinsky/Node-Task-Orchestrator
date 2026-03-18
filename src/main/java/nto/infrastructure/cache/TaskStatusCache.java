package nto.infrastructure.cache;

import nto.core.entities.TaskEntity;
import nto.core.utils.TaskCacheKey;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskStatusCache {
    private final Map<TaskCacheKey, TaskEntity> cache = new ConcurrentHashMap<>();
    private final Map<Long, TaskEntity> tasksCache = new ConcurrentHashMap<>();

    public void put(TaskEntity task) {
        if (task.getServer() != null && task.getScript() != null) {
            TaskCacheKey key = new TaskCacheKey(task.getServer().getId(), task.getScript().getId());
            cache.put(key, task);
            tasksCache.put(task.getId(), task);
        }
    }

    public void evictAllByServerId(Long serverId) {
        cache.keySet().removeIf(key -> key.serverId().equals(serverId));
    }

    public TaskEntity get(Long serverId, Long scriptId) {
        return cache.get(new TaskCacheKey(serverId, scriptId));
    }

    public TaskEntity get(Long taskId) {
        return tasksCache.get(taskId);
    }

    public void evict(Long serverId, Long scriptId) {
        cache.remove(new TaskCacheKey(serverId, scriptId));
        tasksCache.remove(scriptId);
    }
}