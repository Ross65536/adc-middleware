package pt.inesctec.adcauthmiddleware;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

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
}
