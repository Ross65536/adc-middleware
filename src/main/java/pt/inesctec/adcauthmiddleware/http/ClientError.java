package pt.inesctec.adcauthmiddleware.http;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.MediaType;
import pt.inesctec.adcauthmiddleware.HttpException;

/**
 * Represents an HTTP exception. Contains the response body and content-type.
 */
public class ClientError extends IOException {
    public final int statusCode;
    private final String errorMsg;
    private final Optional<String> contentType;

    public ClientError(int statusCode, String errorMsg, Optional<String> contentType) {
        super("Unexpected response code: " + statusCode + ", body: " + errorMsg);
        this.statusCode = statusCode;
        this.errorMsg = errorMsg;
        this.contentType = contentType;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Check whether HTTP response was JSON.
     *
     * @return true when the original HTTP response was JSON type.
     */
    public boolean isJson() {
        return contentType.isPresent() && contentType.get().contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Emit a regular HTTP exception. Used for the spring exception handler.
     *
     * @return exception.
     */
    public HttpException toHttpException() {
        return new HttpException(statusCode, errorMsg, contentType);
    }
}
