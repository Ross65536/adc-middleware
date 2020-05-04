package pt.inesctec.adcauthmiddleware.http;

import org.springframework.http.MediaType;
import pt.inesctec.adcauthmiddleware.HttpException;

import java.io.IOException;
import java.util.Optional;

public class ClientError extends IOException {
  private final int statusCode;
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

  public boolean isJson() {
    return contentType.isPresent() && contentType.get().contains(MediaType.APPLICATION_JSON_VALUE);
  }

  public HttpException toHttpException() {
    return new HttpException(statusCode, errorMsg, contentType);
  }
}
