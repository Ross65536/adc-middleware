package pt.inesctec.adcauthmiddleware;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public final class Utils {

    public static URI buildUrl(String baseUrl, String ... pathParts) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment(pathParts)
                .toUriString();

        return URI.create(url);
    }
}
