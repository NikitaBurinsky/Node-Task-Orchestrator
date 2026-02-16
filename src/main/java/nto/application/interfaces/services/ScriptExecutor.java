package nto.application.interfaces.services;

public interface ScriptExecutor {
    /**
     * Асинхронно выполняет задачу.
     * @param taskId ID задачи (передаем ID, а не Entity, чтобы открыть новую транзакцию)
     */
    void executeAsync(Long taskId);

    // Для демонстрации Race Condition
    long getSuccessCountAtomic();
    long getSuccessCountUnsafe();
}