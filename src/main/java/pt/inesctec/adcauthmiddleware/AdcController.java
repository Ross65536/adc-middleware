package pt.inesctec.adcauthmiddleware;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.db.CacheRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.exceptions.UmaFlowException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

import javax.servlet.http.HttpServletRequest;

@RestController
public class AdcController {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcController.class);

  @Autowired private AdcClient adcClient;
  @Autowired private CacheRepository cacheRepository;
  @Autowired private UmaFlow umaFlow;

  @Autowired
  public AdcController(CacheRepository cacheRepository) throws Exception {
    //    cacheRepository.synchronize();
  }

  @ExceptionHandler(TicketException.class)
  public ResponseEntity ticketHandler(TicketException e) {
    var header = e.buildAuthenticateHeader();

    var headers = new HttpHeaders();
    headers.add(HttpHeaders.WWW_AUTHENTICATE, header);

    return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);
  }

  @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
  @ExceptionHandler(UmaFlowException.class)
  public void umaFlowHandler(Exception e) {
    Logger.info("Uma flow access error: " + e.getMessage());
    Logger.debug("Stacktrace: ", e);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity statusException(ResponseStatusException e) {
    Logger.debug("Stacktrace: ", e);
    return new ResponseEntity(e.getStatus());
  }

  @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
  @ExceptionHandler(Exception.class)
  public void errorHandler(Exception e) {
    Logger.error("Internal error occured: " + e.getMessage(), e);
  }

  @RequestMapping(
      value = "/repertoire/{repertoireId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public String repertoire(HttpServletRequest request, @PathVariable String repertoireId)
      throws Exception {
    var umaId = this.cacheRepository.getRepertoireUmaId(repertoireId);
    exactUmaFlow(request, umaId, "non-existing repertoire in cache " + repertoireId);

    return this.adcClient.getRepertoireAsString(repertoireId);
  }

  // TODO add security
  @RequestMapping(value = "/synchronize", method = RequestMethod.POST)
  public void synchronize() throws Exception {
    this.cacheRepository.synchronize();
  }

  private void exactUmaFlow(
      HttpServletRequest request, String umaId, String errorMsg, String... umaScopes)
      throws Exception {

    if (umaId == null) {
      Logger.info("User tried accessing non-existing resource {}", errorMsg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    var bearer = AdcController.getBearer(request);
    var umaResource = new UmaResource(umaId, umaScopes); // repertoire is access level 3
    this.umaFlow.exactMatchFlow(bearer, umaResource);
  }

  private static String getBearer(HttpServletRequest request) {
    var auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth == null) {
      return null;
    }

    if (!auth.startsWith("Bearer ")) {
      return null;
    }

    return auth.replace("Bearer ", "");
  }
}
