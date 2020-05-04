package pt.inesctec.adcauthmiddleware.utils;

import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.net.URI;
import java.util.stream.Collectors;

public final class Utils {

  private static ValidatorFactory ValidatorFactory = Validation.buildDefaultValidatorFactory();

  public static URI buildUrl(String baseUrl, String... pathParts) {
    String url = UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(pathParts).toUriString();

    return URI.create(url);
  }

  public static void jaxValidate(Object obj) throws Exception {
    var constraints = ValidatorFactory.getValidator().validate(obj);
    if (constraints.size() != 0) {
      var msg = constraints.stream()
          .map(c -> c.getPropertyPath() + " " + c.getMessage())
          .collect(Collectors.joining(", "));
      throw new Exception("Invalid JSON received: " + msg);
    }
  }

  public static void jaxValidateList(Iterable list) throws Exception {
    for (var e : list) {
      Utils.jaxValidate(e);
    }
  }

  public static void assertNotNull(Object arg) throws Exception {
    if (arg == null) {
      throw new Exception("Must not be null");
    }
  }

  public static String getNestedExceptionMessage(Throwable e) {
    var cause = e.getCause();
    if (cause == null) {
      return e.getMessage();
    }

    return Utils.getNestedExceptionMessage(cause);
  }

}
