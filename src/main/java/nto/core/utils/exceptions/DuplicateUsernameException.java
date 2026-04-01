package nto.core.utils.exceptions;

public class DuplicateUsernameException extends ResourceConflictException {
    public DuplicateUsernameException(String message) {
        super(message);
    }
}
