package cz.semester.courseapp.http;

import cz.semester.courseapp.domain.Enrollment;
import cz.semester.courseapp.domain.EnrollmentStatus;

record EnrollmentResponse(Long id, Long studentId, String studentName, EnrollmentStatus status) {

    static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getName(),
                enrollment.getStatus());
    }
}
