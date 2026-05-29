package cz.semester.courseapp.http;

import cz.semester.courseapp.http.validation.ValidSessionWindow;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@ValidSessionWindow
public record AddSessionRequest(@NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt) {
}
