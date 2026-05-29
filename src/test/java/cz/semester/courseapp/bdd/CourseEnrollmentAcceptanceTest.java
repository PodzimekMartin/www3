package cz.semester.courseapp.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import cz.semester.courseapp.app.CourseService;
import cz.semester.courseapp.domain.EnrollmentStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.seed-data=false")
class CourseEnrollmentAcceptanceTest {

    @Autowired
    private CourseService courseService;

    @Test
    void waitlistedStudentIsPromotedAfterSeatIsReleased() {
        var first = courseService.createStudent("Acceptance One", "acceptance-one@example.test");
        var second = courseService.createStudent("Acceptance Two", "acceptance-two@example.test");
        var course = courseService.createCourse("Acceptance Driven DevOps", 1);
        courseService.addSession(
                course.getId(),
                LocalDateTime.parse("2027-05-04T10:00:00"),
                LocalDateTime.parse("2027-05-04T12:00:00"));
        courseService.publishCourse(course.getId());

        var firstEnrollment = courseService.enroll(course.getId(), first.getId());
        var secondEnrollment = courseService.enroll(course.getId(), second.getId());
        assertThat(firstEnrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(secondEnrollment.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);

        var afterCancel = courseService.cancelEnrollment(course.getId(), first.getId());

        assertThat(afterCancel.getEnrollments())
                .singleElement()
                .extracting(enrollment -> enrollment.getStatus())
                .isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(afterCancel.activeEnrollmentCount()).isEqualTo(1);
        assertThat(afterCancel.waitlistCount()).isZero();
    }
}
