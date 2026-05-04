package cz.semester.courseapp.app;

public class NotFoundOrConflictException extends RuntimeException {

    public NotFoundOrConflictException(String message) {
        super(message);
    }
}
