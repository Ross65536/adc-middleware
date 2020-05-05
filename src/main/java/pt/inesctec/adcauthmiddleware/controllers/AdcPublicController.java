package pt.inesctec.adcauthmiddleware.controllers;

import org.apache.tomcat.util.http.fileupload.IOUtils;
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
import pt.inesctec.adcauthmiddleware.SpringUtils;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;

import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;

@RestController
public class AdcPublicController {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcPublicController.class);

  @ExceptionHandler(HttpException.class)
  public ResponseEntity<String> httpExceptionForward(HttpException e) {
    Logger.debug("Stacktrace: ", e);
    return SpringUtils.buildResponse(e.statusCode, e.errorMsg, e.contentType.orElse(null));
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<String> internalErrorHandler(Exception e) {
    Logger.error("Internal error occurred: {}", e.getMessage());
    Logger.info("Stacktrace: ", e);
    return SpringUtils.buildJsonErrorResponse(HttpStatus.BAD_GATEWAY, null);
  }

  @Autowired
  private AdcClient adcClient;

  @RequestMapping(
      value = "/",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> root() throws Exception {
    return forward("");
  }

  @RequestMapping(
      value = "/info",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> info() throws Exception {
    return forward("info");
  }

  @RequestMapping(
      value = "/swagger",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> swagger() throws Exception {
    return forward("swagger");
  }

  private ResponseEntity<StreamingResponseBody> forward(String path) throws Exception {
    var is = SpringUtils.catchForwardingError(() -> this.adcClient.getResource(path));
    return SpringUtils.buildJsonStream((OutputStream os) -> IOUtils.copy(is, os));
  }


}
