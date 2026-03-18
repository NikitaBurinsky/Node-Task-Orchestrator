package nto.core.enums;

public enum TaskStatus {
    PENDING(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3),
    CANCELLED(4);

    private final int code;

    TaskStatus(int code) {
        this.code = code;
    }

    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TaskStatus code: " + code);
    }

    public int getCode() {
        return code;
    }
}
