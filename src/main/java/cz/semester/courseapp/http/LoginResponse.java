package cz.semester.courseapp.http;

import cz.semester.courseapp.app.UserSession;

record LoginResponse(String token, UserSession.Role role, Long studentId, String displayName) {

    static LoginResponse from(UserSession session) {
        return new LoginResponse(session.token(), session.role(), session.studentId(), session.displayName());
    }
}
