package nto.application.interfaces.services;

public interface ScriptExecutor {
    




    void executeAsync(Long taskId);

    boolean ping(Long serverId);

    
    long getSuccessCountAtomic();

    long getSuccessCountUnsafe();
}