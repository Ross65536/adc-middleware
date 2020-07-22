package pt.inesctec.adcauthmiddleware.utils;

import java.net.URI;
import java.util.stream.Collectors;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.springframework.web.util.UriComponentsBuilder;

public final class Utils {

  private static ValidatorFactory ValidatorFactory = Validation.buildDefaultValidatorFactory();

  /**
   * Join URL parts with '/'.
   *
   * @param baseUrl the base url
   * @param pathParts the url parts
   * @return the built URL
   */
  public static URI buildUrl(String baseUrl, String... pathParts) {
    String url = UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(pathParts).toUriString();

    return URI.create(url);
  }

  /**
   * Validate single JAX object for the annotations.
   * @param obj the object
   * @throws Exception when JAX validation fails
   */
  public static void jaxValidate(Object obj) throws Exception {
    var constraints = ValidatorFactory.getValidator().validate(obj);
    if (constraints.size() != 0) {
      var msg =
          constraints.stream()
              .map(c -> c.getPropertyPath() + " " + c.getMessage())
              .collect(Collectors.joining(", "));
      throw new Exception("Invalid JSON received: " + msg);
    }
  }

  /**
   * Validate JAX annotations in the list elements.
   * @param list the source elements.
   * @throws Exception when a JAX validation fails.
   */
  public static void jaxValidateList(Iterable list) throws Exception {
    for (var e : list) {
      Utils.jaxValidate(e);
    }
  }

  /**
   * Assert argument not null.
   *
   * @param arg the argument
   * @throws Exception on error
   */
  public static void assertNotNull(Object arg) throws Exception {
    if (arg == null) {
      throw new Exception("Must not be null");
    }
  }

}
