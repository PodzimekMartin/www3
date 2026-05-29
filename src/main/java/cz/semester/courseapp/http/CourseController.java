package cz.semester.courseapp.http;

import cz.semester.courseapp.app.AuthSessionService;
import cz.semester.courseapp.app.CourseService;
import cz.semester.courseapp.app.UserSession;
import cz.semester.courseapp.domain.CourseStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public StateResponse state(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken) {
        return StateResponse.from(authSessionService.stateFor(authSessionService.require(token(authorization, legacyToken))));
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @Valid @RequestBody CreateStudentRequest request) {
        authSessionService.requireAdmin(token(authorization, legacyToken));
        return StudentResponse.from(courseService.createStudent(request.name(), request.email()));
    }

    @PostMapping("/instructors")
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorResponse createInstructor(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @Valid @RequestBody CreateInstructorRequest request) {
        authSessionService.requireAdmin(token(authorization, legacyToken));
        return InstructorResponse.from(courseService.createInstructor(request.name(), request.email()));
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse createCourse(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @Valid @RequestBody CreateCourseRequest request) {
        String token = token(authorization, legacyToken);
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

    @GetMapping("/courses/search")
    public PageResponse<CourseResponse> searchCourses(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        authSessionService.require(token(authorization, legacyToken));
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by("title").ascending());
        return PageResponse.from(courseService.searchCourses(query, status, pageable).map(CourseResponse::from));
    }

    @PostMapping("/courses/{id}/sessions")
    public CourseResponse addSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @PathVariable Long id,
            @Valid @RequestBody AddSessionRequest request) {
        authSessionService.requireCourseManager(token(authorization, legacyToken), id);
        return CourseResponse.from(courseService.addSession(id, request.startsAt(), request.endsAt()));
    }

    @PostMapping("/courses/{id}/publish")
    public CourseResponse publish(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @PathVariable Long id) {
        authSessionService.requireCourseManager(token(authorization, legacyToken), id);
        return CourseResponse.from(courseService.publishCourse(id));
    }

    @PostMapping("/courses/{id}/enroll")
    public EnrollmentResponse enroll(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @PathVariable Long id,
            @Valid @RequestBody EnrollRequest request) {
        UserSession session = authSessionService.require(token(authorization, legacyToken));
        authSessionService.requireStudentSelf(session, request.studentId());
        return EnrollmentResponse.from(courseService.enroll(id, request.studentId()));
    }

    @DeleteMapping("/courses/{courseId}/enrollments/{studentId}")
    public CourseResponse cancelEnrollment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        UserSession session = authSessionService.require(token(authorization, legacyToken));
        authSessionService.requireEnrollmentManagerOrStudentSelf(session, courseId, studentId);
        return CourseResponse.from(courseService.cancelEnrollment(courseId, studentId));
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @PathVariable Long id) {
        authSessionService.requireCourseManager(token(authorization, legacyToken), id);
        courseService.deleteCourse(id);
    }

    @PatchMapping("/courses/{id}/capacity")
    public CourseResponse changeCapacity(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @PathVariable Long id,
            @Valid @RequestBody ChangeCapacityRequest request) {
        authSessionService.requireCourseManager(token(authorization, legacyToken), id);
        return CourseResponse.from(courseService.changeCapacity(id, request.capacity()));
    }

    @PatchMapping("/students/{id}/blocked")
    public StudentResponse setBlocked(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Auth-Token", required = false) String legacyToken,
            @PathVariable Long id,
            @Valid @RequestBody BlockStudentRequest request) {
        authSessionService.requireAdmin(token(authorization, legacyToken));
        return StudentResponse.from(courseService.setBlocked(id, request.blocked()));
    }

    private String token(String authorization, String legacyToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length());
        }
        return legacyToken;
    }
}
