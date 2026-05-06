package cz.semester.courseapp.http;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
