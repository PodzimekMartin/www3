package cz.semester.courseapp.http;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

record CreateStudentRequest(@NotBlank String name, @Email @NotBlank String email) {
}

record CreateCourseRequest(@NotBlank String title, @Min(1) int capacity) {
}

record AddSessionRequest(@NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt) {
}

record EnrollRequest(@NotNull Long studentId) {
}

record ChangeCapacityRequest(@Min(1) int capacity) {
}

record BlockStudentRequest(boolean blocked) {
}
