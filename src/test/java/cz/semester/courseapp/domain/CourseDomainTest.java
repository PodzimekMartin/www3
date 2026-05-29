package cz.semester.courseapp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CourseDomainTest {

    @Test
    void publishedCourseAcceptsStudentWhenCapacityIsAvailable() {
        Course course = publishedCourse(2);
        Student student = student(1L, "Ada");

        Enrollment enrollment = course.enroll(student);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(course.activeEnrollmentCount()).isEqualTo(1);
    }

    @Test
    void fullCoursePutsStudentOnWaitlist() {
        Course course = publishedCourse(1);

        course.enroll(student(1L, "Ada"));
        Enrollment secondEnrollment = course.enroll(student(2L, "Grace"));

        assertThat(secondEnrollment.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        assertThat(course.waitlistCount()).isEqualTo(1);
    }

    @Test
    void draftCourseCannotAcceptEnrollment() {
        Course course = new Course("Java", 1);

        assertThatThrownBy(() -> course.enroll(student(1L, "Ada")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("nepublikovaneho");
    }

    @Test
    void finishedCourseCannotAcceptEnrollment() {
        Course course = publishedCourse(1);
        LocalDateTime afterSession = LocalDateTime.parse("2027-05-04T13:00:00");

        assertThatThrownBy(() -> course.enroll(student(1L, "Ada"), afterSession))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("termin uz probehl");
    }

    @Test
    void courseCannotBePublishedWithoutSession() {
        Course course = new Course("Java", 1);

        assertThatThrownBy(course::publish)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("bez terminu");
    }

    @Test
    void blockedStudentCannotEnroll() {
        Course course = publishedCourse(1);
        Student student = student(1L, "Ada");
        student.setBlocked(true);

        assertThatThrownBy(() -> course.enroll(student))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Blokovany");
    }

    @Test
    void duplicateEnrollmentIsRejected() {
        Course course = publishedCourse(2);
        Student student = student(1L, "Ada");

        course.enroll(student);

        assertThatThrownBy(() -> course.enroll(student))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("uz je");
    }

    @Test
    void cancelingEnrollmentPromotesFirstWaitlistedStudent() {
        Course course = publishedCourse(1);
        Student first = student(1L, "Ada");
        Student second = student(2L, "Grace");

        course.enroll(first);
        course.enroll(second);
        course.cancelEnrollment(first.getId());

        assertThat(course.activeEnrollmentCount()).isEqualTo(1);
        assertThat(course.waitlistCount()).isZero();
        assertThat(course.getEnrollments())
                .singleElement()
                .extracting(Enrollment::getStudent)
                .isEqualTo(second);
    }

    @Test
    void capacityCannotBeLowerThanActiveEnrollmentCount() {
        Course course = publishedCourse(2);
        course.enroll(student(1L, "Ada"));
        course.enroll(student(2L, "Grace"));

        assertThatThrownBy(() -> course.changeCapacity(1))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("mensi");
    }

    @Test
    void invalidSessionTimeIsRejected() {
        LocalDateTime startsAt = LocalDateTime.parse("2027-05-04T10:00:00");

        assertThatThrownBy(() -> new CourseSession(startsAt, startsAt.minusMinutes(1)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Konec");
    }

    private Course publishedCourse(int capacity) {
        Course course = new Course("Java", capacity);
        course.addSession(new CourseSession(
                LocalDateTime.parse("2027-05-04T10:00:00"),
                LocalDateTime.parse("2027-05-04T12:00:00")));
        course.publish();
        return course;
    }

    private Student student(Long id, String name) {
        Student student = new Student(name, name.toLowerCase() + "@example.test");
        setId(student, id);
        return student;
    }

    private void setId(Student student, Long id) {
        try {
            Field idField = Student.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(student, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
