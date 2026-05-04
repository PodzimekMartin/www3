package cz.semester.courseapp.infra;

import cz.semester.courseapp.app.NotificationGateway;
import cz.semester.courseapp.domain.EnrollmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    @Override
    public void enrollmentChanged(String email, String courseTitle, EnrollmentStatus status) {
        LOGGER.info(
                "event=enrollment_changed email={} course=\"{}\" status={}",
                email,
                courseTitle,
                status);
    }
}
