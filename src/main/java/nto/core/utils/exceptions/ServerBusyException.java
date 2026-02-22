package nto.core.utils.exceptions;

public class ServerBusyException extends RuntimeException {
    public ServerBusyException(String message) {
        super(message);
    }
}