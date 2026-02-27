package nto.core.utils;

public enum ErrorMessages {
    SERVER_NOT_FOUND("Server not found"),
    USER_NOT_FOUND("User not found"),
    SCRIPT_NOT_FOUND("Script not found"),
    TASK_NOT_FOUND("Task not found"),
    UNAUTHORIZED("Unauthorized"),
    ACCESS_DENIED("Access Denied"),
    INVALID_INPUT("Bad credentials"),
    DATABASE_ERROR("Database error"),
    ;

    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

    public String getMessageMany() {
        return "Many -> " + message;
    }

    public String getMessage() {
        return message;
    }
}