package cz.semester.courseapp.http;

import cz.semester.courseapp.app.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/state")
    public StateResponse state() {
        return StateResponse.from(courseService.state());
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return StudentResponse.from(courseService.createStudent(request.name(), request.email()));
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return CourseResponse.from(courseService.createCourse(request.title(), request.capacity()));
    }

    @PostMapping("/courses/{id}/sessions")
    public CourseResponse addSession(
            @PathVariable Long id,
            @Valid @RequestBody AddSessionRequest request) {
        return CourseResponse.from(courseService.addSession(id, request.startsAt(), request.endsAt()));
    }

    @PostMapping("/courses/{id}/publish")
    public CourseResponse publish(@PathVariable Long id) {
        return CourseResponse.from(courseService.publishCourse(id));
    }

    @PostMapping("/courses/{id}/enroll")
    public EnrollmentResponse enroll(
            @PathVariable Long id,
            @Valid @RequestBody EnrollRequest request) {
        return EnrollmentResponse.from(courseService.enroll(id, request.studentId()));
    }

    @DeleteMapping("/courses/{courseId}/enrollments/{studentId}")
    public CourseResponse cancelEnrollment(@PathVariable Long courseId, @PathVariable Long studentId) {
        return CourseResponse.from(courseService.cancelEnrollment(courseId, studentId));
    }

    @PatchMapping("/courses/{id}/capacity")
    public CourseResponse changeCapacity(
            @PathVariable Long id,
            @Valid @RequestBody ChangeCapacityRequest request) {
        return CourseResponse.from(courseService.changeCapacity(id, request.capacity()));
    }

    @PatchMapping("/students/{id}/blocked")
    public StudentResponse setBlocked(
            @PathVariable Long id,
            @Valid @RequestBody BlockStudentRequest request) {
        return StudentResponse.from(courseService.setBlocked(id, request.blocked()));
    }
}
