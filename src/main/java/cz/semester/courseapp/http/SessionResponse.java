package cz.semester.courseapp.http;

import cz.semester.courseapp.domain.CourseSession;
import java.time.LocalDateTime;

record SessionResponse(Long id, LocalDateTime startsAt, LocalDateTime endsAt) {

    static SessionResponse from(CourseSession session) {
        return new SessionResponse(session.getId(), session.getStartsAt(), session.getEndsAt());
    }
}
