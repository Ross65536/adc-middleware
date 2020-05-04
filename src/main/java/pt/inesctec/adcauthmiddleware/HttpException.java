package pt.inesctec.adcauthmiddleware;

import java.util.Optional;

public class HttpException extends Exception {
  public final int statusCode;
  public final String errorMsg;
  public final Optional<String> contentType;

  public HttpException(int statusCode, String errorMsg, Optional<String> contentType) {
    super("Unexpected response code: " + statusCode + ", body: " + errorMsg);
    this.statusCode = statusCode;
    this.errorMsg = errorMsg;
    this.contentType = contentType;
  }
}
