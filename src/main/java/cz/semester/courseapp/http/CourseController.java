package cz.semester.courseapp.http;

import cz.semester.courseapp.app.AuthSessionService;
import cz.semester.courseapp.app.CourseService;
import cz.semester.courseapp.app.UserSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;
    private final AuthSessionService authSessionService;

    public CourseController(CourseService courseService, AuthSessionService authSessionService) {
        this.courseService = courseService;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(authSessionService.login(request.username(), request.password()));
    }

    @GetMapping("/state")
    public StateResponse state(@RequestHeader("X-Auth-Token") String token) {
        return StateResponse.from(authSessionService.stateFor(authSessionService.require(token)));
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody CreateStudentRequest request) {
        authSessionService.requireAdmin(token);
        return StudentResponse.from(courseService.createStudent(request.name(), request.email()));
    }

    @PostMapping("/instructors")
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorResponse createInstructor(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody CreateInstructorRequest request) {
        authSessionService.requireAdmin(token);
        return InstructorResponse.from(courseService.createInstructor(request.name(), request.email()));
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse createCourse(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody CreateCourseRequest request) {
        UserSession session = authSessionService.require(token);
        Long instructorId = session.isInstructor() ? session.instructorId() : request.instructorId();
        if (!session.isAdmin() && !session.isInstructor()) {
            authSessionService.requireAdmin(token);
        }
        if (session.isAdmin() && instructorId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin musi ke kurzu priradit vyucujiciho.");
        }
        return CourseResponse.from(courseService.createCourse(request.title(), request.capacity(), instructorId));
    }

    @PostMapping("/courses/{id}/sessions")
    public CourseResponse addSession(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody AddSessionRequest request) {
        authSessionService.requireCourseManager(token, id);
        return CourseResponse.from(courseService.addSession(id, request.startsAt(), request.endsAt()));
    }

    @PostMapping("/courses/{id}/publish")
    public CourseResponse publish(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        authSessionService.requireCourseManager(token, id);
        return CourseResponse.from(courseService.publishCourse(id));
    }

    @PostMapping("/courses/{id}/enroll")
    public EnrollmentResponse enroll(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody EnrollRequest request) {
        UserSession session = authSessionService.require(token);
        authSessionService.requireStudentSelf(session, request.studentId());
        return EnrollmentResponse.from(courseService.enroll(id, request.studentId()));
    }

    @DeleteMapping("/courses/{courseId}/enrollments/{studentId}")
    public CourseResponse cancelEnrollment(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        UserSession session = authSessionService.require(token);
        authSessionService.requireEnrollmentManagerOrStudentSelf(session, courseId, studentId);
        return CourseResponse.from(courseService.cancelEnrollment(courseId, studentId));
    }

    @PatchMapping("/courses/{id}/capacity")
    public CourseResponse changeCapacity(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody ChangeCapacityRequest request) {
        authSessionService.requireCourseManager(token, id);
        return CourseResponse.from(courseService.changeCapacity(id, request.capacity()));
    }

    @PatchMapping("/students/{id}/blocked")
    public StudentResponse setBlocked(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody BlockStudentRequest request) {
        authSessionService.requireAdmin(token);
        return StudentResponse.from(courseService.setBlocked(id, request.blocked()));
    }
}
