package cz.semester.courseapp.app;

import cz.semester.courseapp.domain.EnrollmentStatus;

public interface NotificationGateway {

    void enrollmentChanged(String email, String courseTitle, EnrollmentStatus status);
}
