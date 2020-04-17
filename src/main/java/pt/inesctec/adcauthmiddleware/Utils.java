package pt.inesctec.adcauthmiddleware;

import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

public final class Utils {

  public static URI buildUrl(String baseUrl, String... pathParts) {
    String url = UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(pathParts).toUriString();

    return URI.create(url);
  }
}
