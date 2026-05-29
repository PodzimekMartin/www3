package cz.semester.courseapp.http.validation;

import cz.semester.courseapp.http.AddSessionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidSessionWindowValidator implements ConstraintValidator<ValidSessionWindow, AddSessionRequest> {

    @Override
    public boolean isValid(AddSessionRequest value, ConstraintValidatorContext context) {
        if (value == null || value.startsAt() == null || value.endsAt() == null) {
            return true;
        }
        return value.endsAt().isAfter(value.startsAt());
    }
}
