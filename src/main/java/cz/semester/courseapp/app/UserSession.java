package cz.semester.courseapp.app;

public record UserSession(String token, Role role, Long studentId, String displayName) {

    public enum Role {
        ADMIN,
        STUDENT
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
