package nl.openvalue.bookstore.command;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Request body for placing an order. The e-mail address may be surrounded by characters at or below U+0020 (the set
 * {@code String.trim()} strips) and is passed on as sent: the domain trims and lower-cases it. The
 * {@code customerEmail} rules judge the trimmed value: at most 254 UTF-16 characters (counted as the domain counts
 * them) and a pattern with no spaces or control characters inside, so a request the domain would refuse is a 400 and
 * never reaches the domain. The one to 20 lines rule is repeated here for the same reason. The quantity rules are not:
 * the order flow owns them.
 */
public record PlaceOrderPayload(
        @NotNull
        @TrimmedLength(max = 254)
        @Pattern(regexp = "[\\x00-\\x20]*[^@\\x00-\\x20]+@[^@\\x00-\\x20]+\\.[^@\\x00-\\x20]+[\\x00-\\x20]*")
        String customerEmail,
        @NotNull @Size(min = 1, max = 20) List<@Valid @NotNull OrderLinePayload> lines) {

    /**
     * The value, once trimmed with {@code String.trim()}, has at most {@code max} UTF-16 characters. Null is valid;
     * {@code @NotNull} covers it.
     */
    @Documented
    @Constraint(validatedBy = TrimmedLengthValidator.class)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR,
            ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TrimmedLength {

        int max();

        String message() default "length after trimming must be at most {max} characters";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /** Validator of {@link TrimmedLength}: counts {@code value.trim().length()}, exactly as the domain does. */
    public static final class TrimmedLengthValidator implements ConstraintValidator<TrimmedLength, String> {

        private int max;

        @Override
        public void initialize(TrimmedLength constraint) {
            this.max = constraint.max();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.trim().length() <= max;
        }
    }
}
