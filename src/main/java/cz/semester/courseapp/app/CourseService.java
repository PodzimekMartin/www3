package cz.semester.courseapp.app;

import cz.semester.courseapp.domain.Course;
import cz.semester.courseapp.domain.CourseSession;
import cz.semester.courseapp.domain.Enrollment;
import cz.semester.courseapp.domain.Instructor;
import cz.semester.courseapp.domain.Student;
import cz.semester.courseapp.infra.CourseRepository;
import cz.semester.courseapp.infra.InstructorRepository;
import cz.semester.courseapp.infra.StudentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final NotificationGateway notificationGateway;

    public CourseService(
            CourseRepository courseRepository,
            StudentRepository studentRepository,
            InstructorRepository instructorRepository,
            NotificationGateway notificationGateway) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.instructorRepository = instructorRepository;
        this.notificationGateway = notificationGateway;
    }

    public Student createStudent(String name, String email) {
        if (studentRepository.existsByEmailIgnoreCase(email)) {
            throw new NotFoundOrConflictException("Student s timto e-mailem uz existuje.");
        }
        return studentRepository.save(new Student(name, email));
    }

    public Instructor createInstructor(String name, String email) {
        if (instructorRepository.existsByEmailIgnoreCase(email)) {
            throw new NotFoundOrConflictException("Vyucujici s timto e-mailem uz existuje.");
        }
        return instructorRepository.save(new Instructor(name, email));
    }

    public Course createCourse(String title, int capacity) {
        return courseRepository.save(new Course(title, capacity));
    }

    public Course createCourse(String title, int capacity, Long instructorId) {
        return courseRepository.save(new Course(title, capacity, instructor(instructorId)));
    }

    public Course addSession(Long courseId, LocalDateTime startsAt, LocalDateTime endsAt) {
        Course course = course(courseId);
        course.addSession(new CourseSession(startsAt, endsAt));
        return course;
    }

    public Course publishCourse(Long courseId) {
        Course course = course(courseId);
        course.publish();
        return course;
    }

    public Enrollment enroll(Long courseId, Long studentId) {
        Course course = course(courseId);
        Student student = student(studentId);
        Enrollment enrollment = course.enroll(student);
        notificationGateway.enrollmentChanged(student.getEmail(), course.getTitle(), enrollment.getStatus());
        return enrollment;
    }

    public Course cancelEnrollment(Long courseId, Long studentId) {
        Course course = course(courseId);
        course.cancelEnrollment(studentId);
        return course;
    }

    public Course changeCapacity(Long courseId, int capacity) {
        Course course = course(courseId);
        course.changeCapacity(capacity);
        return course;
    }

    public void deleteCourse(Long courseId) {
        courseRepository.delete(course(courseId));
    }

    public Student setBlocked(Long studentId, boolean blocked) {
        Student student = student(studentId);
        student.setBlocked(blocked);
        return student;
    }

    @Transactional(readOnly = true)
    public ApplicationState state() {
        return new ApplicationState(
                studentRepository.findAll(),
                instructorRepository.findAll(),
                courseRepository.findAll());
    }

    private Course course(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundOrConflictException("Kurz nebyl nalezen."));
    }

    private Student student(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundOrConflictException("Student nebyl nalezen."));
    }

    private Instructor instructor(Long id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new NotFoundOrConflictException("Vyucujici nebyl nalezen."));
    }

    public record ApplicationState(List<Student> students, List<Instructor> instructors, List<Course> courses) {
    }
}
