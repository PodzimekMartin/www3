package cz.semester.courseapp.http.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSessionWindowValidator.class)
public @interface ValidSessionWindow {

    String message() default "Konec terminu musi byt pozdeji nez zacatek.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
