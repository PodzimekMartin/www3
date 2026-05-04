package cz.semester.courseapp.http;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record CreateStudentRequest(@NotBlank String name, @Email @NotBlank String email) {
}
