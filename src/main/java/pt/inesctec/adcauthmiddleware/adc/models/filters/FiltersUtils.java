package pt.inesctec.adcauthmiddleware.adc.models.filters;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;

/**
 * Utilities for ADC user request validation.
 */
public class FiltersUtils {
    /**
     * Assert that the value is a basic type (integer, number, boolean, string).
     *
     * @param field the user friendly field path for error messages.
     * @param value the value to check.
     * @throws AdcException when assertion fails.
     */
    public static void assertPrimitiveType(String field, Object value) throws AdcException {
        if (!(value instanceof Integer
                || value instanceof Double
                || value instanceof Boolean
                || value instanceof String)) {
            throw new AdcException("'" + field + "' must be a JSON number, boolean or string");
        }
    }

    /**
     * Asserts value is not null.
     *
     * @param field the user friendly field path for error messages.
     * @param value the value to check.
     * @throws AdcException when assertion fails.
     */
    public static void assertNonNull(String field, Object value) throws AdcException {
        if (value == null) {
            throw new AdcException(field + " must exist or not be null");
        }
    }

    /**
     * Asserts value is a number (integer or real).
     *
     * @param field the user friendly field path for error messages.
     * @param value the value to check.
     * @throws AdcException when assertion fails.
     */
    public static void assertNumber(String field, Object value) throws AdcException {
        if (!(value instanceof Integer || value instanceof Double)) {
            throw new AdcException("'" + field + "' must be a JSON number");
        }
    }

    /**
     * Asserts value is a string.
     *
     * @param field the user friendly field path for error messages.
     * @param value the value to check.
     * @throws AdcException when assertion fails.
     */
    public static void assertString(String field, Object value) throws AdcException {
        if (!(value instanceof String)) {
            throw new AdcException("'" + field + "' must be a JSON string");
        }
    }
}
