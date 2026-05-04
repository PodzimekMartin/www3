package cz.semester.courseapp.http;

import cz.semester.courseapp.app.CourseService.ApplicationState;
import cz.semester.courseapp.domain.Course;
import cz.semester.courseapp.domain.CourseSession;
import cz.semester.courseapp.domain.CourseStatus;
import cz.semester.courseapp.domain.Enrollment;
import cz.semester.courseapp.domain.EnrollmentStatus;
import cz.semester.courseapp.domain.Student;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

record StateResponse(List<StudentResponse> students, List<CourseResponse> courses) {

    static StateResponse from(ApplicationState state) {
        return new StateResponse(
                state.students().stream().map(StudentResponse::from).toList(),
                state.courses().stream().map(CourseResponse::from).toList());
    }
}

record StudentResponse(Long id, String name, String email, boolean blocked) {

    static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getName(), student.getEmail(), student.isBlocked());
    }
}

record CourseResponse(
        Long id,
        String title,
        int capacity,
        CourseStatus status,
        long enrolledCount,
        long waitlistCount,
        List<SessionResponse> sessions,
        List<EnrollmentResponse> enrollments) {

    static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getCapacity(),
                course.getStatus(),
                course.activeEnrollmentCount(),
                course.waitlistCount(),
                course.getSessions().stream().map(SessionResponse::from).toList(),
                course.getEnrollments().stream()
                        .sorted(Comparator.comparing(Enrollment::getCreatedAt))
                        .map(EnrollmentResponse::from)
                        .toList());
    }
}

record SessionResponse(Long id, LocalDateTime startsAt, LocalDateTime endsAt) {

    static SessionResponse from(CourseSession session) {
        return new SessionResponse(session.getId(), session.getStartsAt(), session.getEndsAt());
    }
}

record EnrollmentResponse(Long id, Long studentId, String studentName, EnrollmentStatus status) {

    static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getName(),
                enrollment.getStatus());
    }
}
