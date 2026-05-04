package cz.semester.courseapp.app;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cz.semester.courseapp.domain.EnrollmentStatus;
import cz.semester.courseapp.domain.Student;
import cz.semester.courseapp.infra.CourseRepository;
import cz.semester.courseapp.infra.StudentRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(CourseService.class)
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @MockBean
    private NotificationGateway notificationGateway;

    @Test
    void enrollmentSavesCourseStateAndSendsNotification() {
        Student student = courseService.createStudent("Ada Lovelace", "ada@example.test");
        Long courseId = courseService.createCourse("Spring Boot", 1).getId();
        courseService.addSession(
                courseId,
                LocalDateTime.parse("2026-05-04T10:00:00"),
                LocalDateTime.parse("2026-05-04T12:00:00"));
        courseService.publishCourse(courseId);

        courseService.enroll(courseId, student.getId());

        verify(notificationGateway).enrollmentChanged(
                eq("ada@example.test"),
                eq("Spring Boot"),
                eq(EnrollmentStatus.ENROLLED));
        var savedCourse = courseRepository.findById(courseId).orElseThrow();
        var savedStudent = studentRepository.findById(student.getId()).orElseThrow();
        assert savedCourse.activeEnrollmentCount() == 1;
        assert !savedStudent.isBlocked();
    }
}
