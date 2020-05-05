package pt.inesctec.adcauthmiddleware.adc.models.filters;

import pt.inesctec.adcauthmiddleware.adc.models.AdcException;

public class FiltersUtils {
  public static void assertPrimitiveType(String field, Object value) throws AdcException {
    if (!(value instanceof Integer || value instanceof Double || value instanceof Boolean || value instanceof String)) {
      throw new AdcException("'" + field + "' must be a JSON number, boolean or string");
    }
  }

  public static void assertNonNull(String field, Object value) throws AdcException {
    if (value == null) {
      throw new AdcException(field + " must exist or not be null");
    }
  }

  public static void assertNumber(String field, Object value) throws AdcException {
    if (!(value instanceof Integer || value instanceof Double)) {
      throw new AdcException("'" + field + "' must be a JSON number");
    }
  }

  public static void assertString(String field, Object value) throws AdcException {
    if (!(value instanceof String)) {
      throw new AdcException("'" + field + "' must be a JSON string");
    }
  }
}
