package cz.semester.courseapp.app;

import cz.semester.courseapp.domain.Course;
import cz.semester.courseapp.domain.CourseStatus;
import cz.semester.courseapp.domain.Student;
import cz.semester.courseapp.infra.CourseRepository;
import cz.semester.courseapp.infra.StudentRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthSessionService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String STUDENT_PASSWORD = "student123";

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final ConcurrentMap<String, UserSession> sessions = new ConcurrentHashMap<>();

    public AuthSessionService(StudentRepository studentRepository, CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    public UserSession login(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (ADMIN_USERNAME.equals(normalizedUsername) && ADMIN_PASSWORD.equals(password)) {
            return remember(UserSession.Role.ADMIN, null, "Administrator");
        }

        Student student = studentRepository.findByEmailIgnoreCase(normalizedUsername)
                .orElseThrow(() -> unauthorized("Neplatne prihlasovaci udaje."));
        if (!STUDENT_PASSWORD.equals(password)) {
            throw unauthorized("Neplatne prihlasovaci udaje.");
        }
        return remember(UserSession.Role.STUDENT, student.getId(), student.getName());
    }

    public UserSession require(String token) {
        if (token == null || token.isBlank()) {
            throw unauthorized("Nejprve se prihlas.");
        }
        UserSession session = sessions.get(token);
        if (session == null) {
            throw unauthorized("Prihlaseni vyprselo nebo je neplatne.");
        }
        return session;
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

    public CourseService.ApplicationState stateFor(UserSession session) {
        if (session.isAdmin()) {
            return new CourseService.ApplicationState(studentRepository.findAll(), courseRepository.findAll());
        }

        Student student = studentRepository.findById(session.studentId())
                .orElseThrow(() -> unauthorized("Studentsky ucet uz neexistuje."));
        List<Course> visibleCourses = courseRepository.findAll().stream()
                .filter(course -> course.getStatus() == CourseStatus.PUBLISHED || course.hasStudent(student.getId()))
                .toList();
        return new CourseService.ApplicationState(List.of(student), visibleCourses);
    }

    private UserSession remember(UserSession.Role role, Long studentId, String displayName) {
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(token, role, studentId, displayName);
        sessions.put(token, session);
        return session;
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
}
