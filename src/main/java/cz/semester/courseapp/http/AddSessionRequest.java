package cz.semester.courseapp.http;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

record AddSessionRequest(@NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt) {
}
