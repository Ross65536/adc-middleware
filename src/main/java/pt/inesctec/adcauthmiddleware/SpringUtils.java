package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.http.ClientError;
import pt.inesctec.adcauthmiddleware.http.Json;
import pt.inesctec.adcauthmiddleware.utils.ThrowingProducer;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

public final class SpringUtils {

  static String getBearer(HttpServletRequest request) {
    var auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth == null) {
      return null;
    }

    if (!auth.startsWith("Bearer ")) {
      return null;
    }

    return auth.replace("Bearer ", "");
  }

  static InputStream catchForwardingError(ThrowingProducer<InputStream, Exception> httpRequest) throws Exception {
    try {
      return httpRequest.get();
    } catch (ClientError e) {
      throw e.toHttpException();
    }
  }

  static HttpException buildHttpException(HttpStatus status, String msg) {
    var json = errorToJson(status.value(), msg);
    return new HttpException(status.value(), json, Optional.of(MediaType.APPLICATION_JSON_VALUE));
  }

  static ResponseEntity<String> buildResponse(int status, String msg, String contentType) {
    var headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_TYPE, contentType);
    var httpStatus = HttpStatus.valueOf(status);

    return new ResponseEntity<>(msg, headers, httpStatus);
  }

  static ResponseEntity<String> buildJsonErrorResponse(HttpStatus status, String msg) {
    return buildJsonErrorResponse(status, msg, ImmutableMap.of());
  }

  static ResponseEntity<String> buildJsonErrorResponse(
      HttpStatus status, String msg, Map<String, String> headers) {
    var responseHeaders = new HttpHeaders();
    responseHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.forEach(responseHeaders::set);
    var json = errorToJson(status.value(), msg);

    return new ResponseEntity<>(json, responseHeaders, status);
  }

  static ResponseEntity<StreamingResponseBody> buildJsonStream(
      StreamingResponseBody streamer) {
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(streamer);
  }

  private static String errorToJson(int statusCode, String msg) {
    Map<String, Object> error = ImmutableMap.of(
        "status", statusCode,
        "message", msg
    );
    try {
      return Json.toJson(error);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to create json");
    }
  }
}
