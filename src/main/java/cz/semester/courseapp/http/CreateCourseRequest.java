package cz.semester.courseapp.http;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

record CreateCourseRequest(@NotBlank String title, @Min(1) int capacity, Long instructorId) {
}
