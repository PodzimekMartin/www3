package cz.semester.courseapp.http;

import cz.semester.courseapp.domain.Course;
import cz.semester.courseapp.domain.CourseStatus;
import cz.semester.courseapp.domain.Enrollment;
import java.util.Comparator;
import java.util.List;

record CourseResponse(
        Long id,
        String title,
        int capacity,
        CourseStatus status,
        Long instructorId,
        String instructorName,
        String instructorEmail,
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
                course.getInstructor() == null ? null : course.getInstructor().getId(),
                course.getInstructor() == null ? "Neprirazeno" : course.getInstructor().getName(),
                course.getInstructor() == null ? "" : course.getInstructor().getEmail(),
                course.activeEnrollmentCount(),
                course.waitlistCount(),
                course.getSessions().stream().map(SessionResponse::from).toList(),
                course.getEnrollments().stream()
                        .sorted(Comparator.comparing(Enrollment::getCreatedAt))
                        .map(EnrollmentResponse::from)
                        .toList());
    }
}
