package pt.inesctec.adcauthmiddleware.http;

import java.net.http.HttpClient;

public class HttpFacade {
    public static HttpClient Client = HttpClient.newBuilder()
            .build();


}
