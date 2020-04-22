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
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
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
  private final AdcClient adcClient;

  private UmaFlow umaFlow;

  @Autowired
  public AdcController(AdcConfiguration adcConfig, UmaConfig umaConfig) throws Exception {
    this.adcClient = new AdcClient(adcConfig);

    var umaClient = new UmaClient(umaConfig);
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
      value = "/repertoire/{repertoireId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity repertoire(HttpServletRequest request, @PathVariable String repertoireId)
      throws Exception {
    final String UMA_RESOURCE_ID = "87e43a0e-9108-41ac-a9da-bee2e3b9bb12";

    var bearer = AdcController.getBearer(request);
    var umaResource = new UmaResource(UMA_RESOURCE_ID, UmaScopes.SEQUENCE.toString()); // repertoire is access level 3
    this.umaFlow.exactMatchFlow(bearer, umaResource);

    var response = this.adcClient.getRepertoireAsString(repertoireId);
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


}
