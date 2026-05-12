package cz.semester.courseapp.app;

public record UserSession(String token, Role role, Long studentId, Long instructorId, String displayName) {

    public enum Role {
        ADMIN,
        STUDENT,
        INSTRUCTOR
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isInstructor() {
        return role == Role.INSTRUCTOR;
    }
}
