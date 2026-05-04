package cz.semester.courseapp.http;

import jakarta.validation.constraints.Min;

record ChangeCapacityRequest(@Min(1) int capacity) {
}
