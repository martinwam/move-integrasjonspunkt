package no.difi.meldingsutveksling.validation;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.util.Set;

public class Asserter {

    private final Validator validator;

    public Asserter(Validator validator) {
        this.validator = validator;
    }

    /**
     * Asserts that all all constraints on {@code object} is valid.
     *
     * @param object object to validate
     * @param groups the group or list of groups targeted for validation (defaults to
     *               {@link Default})
     * @throws ConstraintViolationException if the input is not valid
     * @throws IllegalArgumentException     if object is {@code null}
     *                                      or if {@code null} is passed to the varargs groups
     * @throws ConstraintViolationException if a non recoverable error happens
     *                                      during the validation process
     */
    public <T> void isValid(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = validator.validate(object, groups);
        if (!violations.isEmpty()) {
            String message = String.format("The following violations where found when validating '%s':%n %s", object, violations);
            throw new ConstraintViolationException(message, violations);
        }
    }
}
