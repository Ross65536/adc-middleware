package pt.inesctec.adcauthmiddleware;

import java.net.URI;
import java.net.http.HttpRequest;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.inesctec.adcauthmiddleware.adc.UmaScopes;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.exceptions.UmaFlowException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

import javax.servlet.http.HttpServletRequest;


@RestController
public class AdcController {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcController.class);

  private final UmaClient umaClient;
  private AdcConfiguration adcConfig;
  private UmaFlow umaFlow;

  @Autowired
  public AdcController(AdcConfiguration adcConfig, UmaConfig umaConfig) throws Exception {
    this.adcConfig = adcConfig;

    this.umaClient = new UmaClient(umaConfig);
    this.umaFlow = new UmaFlow(umaClient);
  }

  @ExceptionHandler(TicketException.class)
  public ResponseEntity ticketHandler(TicketException e) {
    var header = e.buildAuthenticateHeader();

    var headers = new HttpHeaders();
    headers.add(HttpHeaders.WWW_AUTHENTICATE, header);

    return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);
  }

  @ResponseStatus(value=HttpStatus.UNAUTHORIZED)
  @ExceptionHandler(UmaFlowException.class)
  public void umaFlowHandler(Exception e) {
    Logger.info("Uma flow access error: " + e.getMessage());
    Logger.debug("Stacktrace: ", e);
  }

  @ResponseStatus(value=HttpStatus.UNAUTHORIZED)
  @ExceptionHandler(Exception.class)
  public void errorHandler(Exception e) {
    Logger.error("Internal error occured: " + e.getMessage(), e);
  }

  @RequestMapping(
      value = "/study/{studyId}/repertoire/{repertoireId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity repertoire(HttpServletRequest request, @PathVariable String studyId, @PathVariable String repertoireId)
      throws Exception {
    final String UMA_RESOURCE_ID = "87e43a0e-9108-41ac-a9da-bee2e3b9bb12";

    var bearer = AdcController.getBearer(request);
    var umaResource = new UmaResource(UMA_RESOURCE_ID, UmaScopes.SEQUENCE.toString()); // repertoire is level 3
    this.umaFlow.exactMatchFlow(bearer, umaResource);

    final URI uri = this.getResourceServerPath("study", studyId, "repertoire", repertoireId);

    HttpRequest request2 = new HttpRequestBuilderFacade()
        .getJson(uri)
        .build();
    var response = HttpFacade.makeExpectJsonStringRequest(request2);

    return new ResponseEntity(response, HttpStatus.OK);
  }

  private static String getBearer(HttpServletRequest request) {
    var auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth == null) {
      return null;
    }

    if (! auth.startsWith("Bearer ")) {
      return null;
    }

    return auth.replace("Bearer ", "");
  }

  private URI getResourceServerPath(String... parts) {
    final String basePath = adcConfig.getResourceServerUrl();
    return Utils.buildUrl(basePath, parts);
  }
}
