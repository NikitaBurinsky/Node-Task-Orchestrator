package nto.core.utils;

/**
 * Составной ключ для индекса задач.
 * Автоматически реализует equals() и hashCode() для всех компонентов.
 */
public record TaskCacheKey(Long serverId, Long scriptId) {
}