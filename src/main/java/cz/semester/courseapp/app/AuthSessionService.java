package cz.semester.courseapp.app;

import cz.semester.courseapp.domain.Course;
import cz.semester.courseapp.domain.Instructor;
import cz.semester.courseapp.domain.Student;
import cz.semester.courseapp.infra.CourseRepository;
import cz.semester.courseapp.infra.InstructorRepository;
import cz.semester.courseapp.infra.StudentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthSessionService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD_HASH =
            "$2a$10$mYFKpPFSEWHb7kpG678ytOQagCd/FJdKJq4n1NkgU0B2OgDXSP8ia";
    private static final String STUDENT_PASSWORD_HASH =
            "$2a$10$tZQfPtUr8nAqpcRFS4lmM.542wGlvPezSSRUOEnEYYJ0Un6R1FmRe";
    private static final String INSTRUCTOR_PASSWORD_HASH =
            "$2a$10$9auy09mOGvEccB5U.EFBbum7WR66sqVU31cFlbFP2YI8ql969ZMNC";

    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final CourseRepository courseRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthSessionService(
            StudentRepository studentRepository,
            InstructorRepository instructorRepository,
            CourseRepository courseRepository,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.instructorRepository = instructorRepository;
        this.courseRepository = courseRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public UserSession login(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (ADMIN_USERNAME.equals(normalizedUsername) && passwordMatches(password, ADMIN_PASSWORD_HASH)) {
            return remember(UserSession.Role.ADMIN, null, null, "Administrator");
        }

        if (passwordMatches(password, INSTRUCTOR_PASSWORD_HASH)) {
            Instructor instructor = instructorRepository.findByEmailIgnoreCase(normalizedUsername)
                    .orElseThrow(() -> unauthorized("Neplatne prihlasovaci udaje."));
            return remember(UserSession.Role.INSTRUCTOR, null, instructor.getId(), instructor.getName());
        }

        if (passwordMatches(password, STUDENT_PASSWORD_HASH)) {
            Student student = studentRepository.findByEmailIgnoreCase(normalizedUsername)
                    .orElseThrow(() -> unauthorized("Neplatne prihlasovaci udaje."));
            return remember(UserSession.Role.STUDENT, student.getId(), null, student.getName());
        }

        throw unauthorized("Neplatne prihlasovaci udaje.");
    }

    public UserSession require(String token) {
        if (token == null || token.isBlank()) {
            throw unauthorized("Nejprve se prihlas.");
        }
        return jwtTokenService.parse(token);
    }

    public UserSession requireAdmin(String token) {
        UserSession session = require(token);
        if (!session.isAdmin()) {
            throw forbidden("Tato akce je povolena pouze administratorovi.");
        }
        return session;
    }

    public void requireStudentSelf(UserSession session, Long studentId) {
        if (session.isAdmin()) {
            return;
        }
        if (!studentId.equals(session.studentId())) {
            throw forbidden("Student muze pracovat pouze se svym uctem.");
        }
    }

    public UserSession requireCourseManager(String token, Long courseId) {
        UserSession session = require(token);
        if (session.isAdmin()) {
            return session;
        }
        if (session.isInstructor() && isInstructorCourse(session, courseId)) {
            return session;
        }
        throw forbidden("Tato akce je povolena pouze administratorovi nebo vyucujicimu kurzu.");
    }

    public void requireEnrollmentManagerOrStudentSelf(UserSession session, Long courseId, Long studentId) {
        if (session.isAdmin() || (session.isInstructor() && isInstructorCourse(session, courseId))) {
            return;
        }
        requireStudentSelf(session, studentId);
    }

    public CourseService.ApplicationState stateFor(UserSession session) {
        if (session.isAdmin()) {
            return new CourseService.ApplicationState(
                    studentRepository.findAll(),
                    instructorRepository.findAll(),
                    courseRepository.findAll());
        }

        if (session.isInstructor()) {
            Instructor instructor = instructorRepository.findById(session.instructorId())
                    .orElseThrow(() -> unauthorized("Ucet vyucujiciho uz neexistuje."));
            List<Course> visibleCourses = courseRepository.findAll().stream()
                    .filter(course -> course.getInstructor() != null
                            && course.getInstructor().getId().equals(instructor.getId()))
                    .toList();
            return new CourseService.ApplicationState(List.of(), List.of(instructor), visibleCourses);
        }

        Student student = studentRepository.findById(session.studentId())
                .orElseThrow(() -> unauthorized("Studentsky ucet uz neexistuje."));
        LocalDateTime now = LocalDateTime.now();
        List<Course> visibleCourses = courseRepository.findAll().stream()
                .filter(course -> course.isBookable(now) || course.hasStudent(student.getId()))
                .toList();
        return new CourseService.ApplicationState(List.of(student), List.of(), visibleCourses);
    }

    private boolean isInstructorCourse(UserSession session, Long courseId) {
        return courseRepository.findById(courseId)
                .map(Course::getInstructor)
                .map(Instructor::getId)
                .filter(session.instructorId()::equals)
                .isPresent();
    }

    private UserSession remember(UserSession.Role role, Long studentId, Long instructorId, String displayName) {
        UserSession session = new UserSession("", role, studentId, instructorId, displayName);
        return new UserSession(jwtTokenService.create(session), role, studentId, instructorId, displayName);
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        return rawPassword != null && passwordEncoder.matches(rawPassword, passwordHash);
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
