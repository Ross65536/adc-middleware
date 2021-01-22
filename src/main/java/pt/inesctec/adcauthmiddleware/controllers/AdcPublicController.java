package pt.inesctec.adcauthmiddleware.controllers;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.HttpException;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * class responsible for the unprotected endpoints. Performs forwarding for these endpoints to the repository.
 */
@RestController
public class AdcPublicController {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcPublicController.class);
  @Autowired private AppConfig appConfig;
  @Autowired private AdcClient adcClient;
  @Autowired private CsvConfig csvConfig;

  /**
   * Returns forwarding error status and body when the repository returns a non-OK status code.
   * @param e Exception
   * @return status + body + contentType from the erroring repository response.
   */
  @ExceptionHandler(HttpException.class)
  public ResponseEntity<String> httpExceptionForward(HttpException e) {
    Logger.debug("Stacktrace: ", e);
    return SpringUtils.buildResponse(e.statusCode, e.errorMsg, e.contentType.orElse(null));
  }

  /**
   * Logs internal errors. An error can indicate a logic error.
   * @param e Exception
   * @return 502 status code
   */
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<String> internalErrorHandler(Exception e) {
    Logger.error("Internal error occurred: {}", e.getMessage());
    Logger.debug("Stacktrace: ", e);
    return SpringUtils.buildJsonErrorResponse(HttpStatus.BAD_GATEWAY, null);
  }

  /**
   * The root ADC v1 endpoint. Unprotected. Forwarded from the repository.
   *
   * @return the repository response.
   * @throws Exception on internal error.
   */
  @RequestMapping(
      value = "/",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> root() throws Exception {
    return forward("");
  }

  /**
   * The root ADC v1 endpoint. Unprotected. Forwarded from the repository.
   *
   * @return the repository response.
   * @throws Exception on internal error.
   */
  @RequestMapping(
      value = "/info",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> info() throws Exception {
    return forward("info");
  }

  /**
   * The public fields endpoint. Not part of ADC v1. Unprotected. Extension of the middleware.
   *
   * @return the public fields for each resource type
   */
  @RequestMapping(value = "/public_fields", method = RequestMethod.GET)
  public Map<FieldClass, Set<String>> publicFields() {

    var map = new HashMap<FieldClass, Set<String>>();
    for (var adcClass : FieldClass.values()) {
      var fields = this.csvConfig.getPublicFields(adcClass);
      map.put(adcClass, fields);
    }

    return map;
  }

  /**
   * The forwarding logic.
   *
   * @param path the API subpath fragment.
   * @return the spring response.
   * @throws Exception on HTTP error or if public endpoints are disabled.
   */
  private ResponseEntity<StreamingResponseBody> forward(String path) throws Exception {
    if (! appConfig.isPublicEndpointsEnabled()) {
      throw SpringUtils.buildHttpException(
          HttpStatus.NOT_IMPLEMENTED, "public endpoints not supported for current repository");
    }

    var is = SpringUtils.catchForwardingError(() -> this.adcClient.getResource(path));
    return SpringUtils.buildJsonStream(is);
  }
}
