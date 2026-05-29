package cz.semester.courseapp.http;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CourseControllerIntegrationTest {

    private static final String AUTH_HEADER = "X-Auth-Token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void studentCanMoveFromWaitlistAfterSeatIsReleased() throws Exception {
        long firstStudent = createStudent("Ada Lovelace", "ada-http@example.test");
        long secondStudent = createStudent("Grace Hopper", "grace-http@example.test");
        long course = createCourse("DevOps", 1);
        addSession(course);
        publish(course);

        enroll(course, firstStudent, "ada-http@example.test", "ENROLLED");
        enroll(course, secondStudent, "grace-http@example.test", "WAITLISTED");

        mockMvc.perform(delete("/api/courses/{courseId}/enrollments/{studentId}", course, firstStudent)
                        .header(AUTH_HEADER, loginStudent("ada-http@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCount").value(1))
                .andExpect(jsonPath("$.waitlistCount").value(0))
                .andExpect(jsonPath("$.enrollments[0].studentId").value(secondStudent));
    }

    @Test
    void invalidEnrollmentReturnsConsistentError() throws Exception {
        long student = createStudent("Linus Torvalds", "linus-http@example.test");
        long course = createCourse("Linux Internals", 1);

        mockMvc.perform(post("/api/courses/{id}/enroll", course)
                        .header(AUTH_HEADER, loginStudent("linus-http@example.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Student se nemuze zapsat do nepublikovaneho kurzu."));
    }

    @Test
    void stateEndpointReturnsStudentsAndCourses() throws Exception {
        createStudent("Katherine Johnson", "katherine-http@example.test");
        createCourse("Algorithms", 3);

        mockMvc.perform(get("/api/state")
                        .header(AUTH_HEADER, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students.length()").value(1))
                .andExpect(jsonPath("$.courses.length()").value(1));
    }

    @Test
    void jwtTokenCanAuthorizeBearerRequest() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/state")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void coursesCanBeSearchedWithPaging() throws Exception {
        createCourse("Advanced Java", 3);
        createCourse("DevOps Basics", 2);

        mockMvc.perform(get("/api/courses/search")
                        .header("Authorization", "Bearer " + adminToken())
                        .param("query", "java")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Advanced Java"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void studentCannotCreateCourse() throws Exception {
        createStudent("Security Student", "security-http@example.test");

        mockMvc.perform(post("/api/courses")
                        .header(AUTH_HEADER, loginStudent("security-http@example.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Forbidden Course", "capacity": 2}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tato akce je povolena pouze administratorovi."));
    }

    @Test
    void instructorCanCreateAndManageOwnCourse() throws Exception {
        long instructor = createInstructor("Teacher One", "teacher-one@example.test");
        String teacherToken = loginInstructor("teacher-one@example.test");

        String response = mockMvc.perform(post("/api/courses")
                        .header(AUTH_HEADER, teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Teacher Course", "capacity": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instructorId").value(instructor))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long course = JsonTestSupport.idFrom(response);

        mockMvc.perform(post("/api/courses/{id}/sessions", course)
                        .header(AUTH_HEADER, teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt": "2027-05-04T10:00:00", "endsAt": "2027-05-04T12:00:00"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void adminAssignsInstructorWhenCreatingCourse() throws Exception {
        long instructor = createInstructor("Teacher Two", "teacher-two@example.test");

        mockMvc.perform(post("/api/courses")
                        .header(AUTH_HEADER, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Assigned Course", "capacity": 3, "instructorId": %d}
                                """.formatted(instructor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instructorName").value("Teacher Two"));
    }

    @Test
    void instructorCanDeleteOwnCourse() throws Exception {
        createInstructor("Teacher Three", "teacher-three@example.test");
        String teacherToken = loginInstructor("teacher-three@example.test");

        String response = mockMvc.perform(post("/api/courses")
                        .header(AUTH_HEADER, teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Disposable Course", "capacity": 2}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long course = JsonTestSupport.idFrom(response);

        mockMvc.perform(delete("/api/courses/{id}", course)
                        .header(AUTH_HEADER, teacherToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/state")
                        .header(AUTH_HEADER, teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses.length()").value(0));
    }

    private long createStudent(String name, String email) throws Exception {
        String location = mockMvc.perform(post("/api/students")
                        .header(AUTH_HEADER, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "%s"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.idFrom(location);
    }

    private long createInstructor(String name, String email) throws Exception {
        String response = mockMvc.perform(post("/api/instructors")
                        .header(AUTH_HEADER, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "%s"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.idFrom(response);
    }

    private long createCourse(String title, int capacity) throws Exception {
        String suffix = Long.toString(System.nanoTime());
        long instructor = createInstructor("Default Teacher", "default-teacher-" + suffix + "@example.test");
        String response = mockMvc.perform(post("/api/courses")
                        .header(AUTH_HEADER, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "capacity": %d, "instructorId": %d}
                                """.formatted(title, capacity, instructor)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.idFrom(response);
    }

    private void addSession(long course) throws Exception {
        mockMvc.perform(post("/api/courses/{id}/sessions", course)
                        .header(AUTH_HEADER, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt": "2027-05-04T10:00:00", "endsAt": "2027-05-04T12:00:00"}
                                """))
                .andExpect(status().isOk());
    }

    private void publish(long course) throws Exception {
        mockMvc.perform(post("/api/courses/{id}/publish", course)
                        .header(AUTH_HEADER, adminToken()))
                .andExpect(status().isOk());
    }

    private void enroll(long course, long student, String email, String expectedStatus) throws Exception {
        mockMvc.perform(post("/api/courses/{id}/enroll", course)
                        .header(AUTH_HEADER, loginStudent(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "admin123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonTestSupport.tokenFrom(response);
        org.assertj.core.api.Assertions.assertThat(token.split("\\.")).hasSize(3);
        return token;
    }

    private String loginInstructor(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "teacher123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.tokenFrom(response);
    }

    private String loginStudent(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "student123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.tokenFrom(response);
    }
}
