package cz.semester.courseapp.http;

import jakarta.validation.constraints.NotNull;

record EnrollRequest(@NotNull Long studentId) {
}
