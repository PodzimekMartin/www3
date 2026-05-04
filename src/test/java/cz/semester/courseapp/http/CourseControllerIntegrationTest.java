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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void studentCanMoveFromWaitlistAfterSeatIsReleased() throws Exception {
        long firstStudent = createStudent("Ada Lovelace", "ada-http@example.test");
        long secondStudent = createStudent("Grace Hopper", "grace-http@example.test");
        long course = createCourse("DevOps", 1);
        addSession(course);
        publish(course);

        enroll(course, firstStudent, "ENROLLED");
        enroll(course, secondStudent, "WAITLISTED");

        mockMvc.perform(delete("/api/courses/{courseId}/enrollments/{studentId}", course, firstStudent))
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

        mockMvc.perform(get("/api/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students.length()").value(1))
                .andExpect(jsonPath("$.courses.length()").value(1));
    }

    private long createStudent(String name, String email) throws Exception {
        String location = mockMvc.perform(post("/api/students")
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

    private long createCourse(String title, int capacity) throws Exception {
        String response = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "capacity": %d}
                                """.formatted(title, capacity)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.idFrom(response);
    }

    private void addSession(long course) throws Exception {
        mockMvc.perform(post("/api/courses/{id}/sessions", course)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt": "2026-05-04T10:00:00", "endsAt": "2026-05-04T12:00:00"}
                                """))
                .andExpect(status().isOk());
    }

    private void publish(long course) throws Exception {
        mockMvc.perform(post("/api/courses/{id}/publish", course))
                .andExpect(status().isOk());
    }

    private void enroll(long course, long student, String expectedStatus) throws Exception {
        mockMvc.perform(post("/api/courses/{id}/enroll", course)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }
}
