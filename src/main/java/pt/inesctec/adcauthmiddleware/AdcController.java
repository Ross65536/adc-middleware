package pt.inesctec.adcauthmiddleware;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.ResourceJsonMapper;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.config.csv.AccessScope;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.db.DbRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.exceptions.UmaFlowException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class AdcController {
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcController.class);

  @Autowired private AdcClient adcClient;
  @Autowired private DbRepository dbRepository;
  @Autowired private UmaFlow umaFlow;
  @Autowired private UmaClient umaClient;
  @Autowired private CsvConfig csvConfig;

  @Autowired
  public AdcController(DbRepository dbRepository) throws Exception {
    //    cacheRepository.synchronize();
  }

  private static final Pattern JsonErrorPattern =
      Pattern.compile(".*line: (\\d+), column: (\\d+).*");

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<HttpError> badInputHandler(HttpMessageNotReadableException e) {
    Logger.info("User input JSON error: {}", e.getMessage());

    // TODO improve returned error MSG
    var msg = "";
    var matcher = JsonErrorPattern.matcher(e.getMessage());
    if (matcher.find()) {
      msg =
          String.format(
              " (malformed or invalid schema) at: line %s, column %s",
              matcher.group(1), matcher.group(2));
    }

    return AdcController.buildError(HttpStatus.BAD_REQUEST, "Invalid input JSON" + msg);
  }

  @ExceptionHandler(TicketException.class)
  public ResponseEntity<HttpError> ticketHandler(TicketException e) {
    var headers = ImmutableMap.of(HttpHeaders.WWW_AUTHENTICATE, e.buildAuthenticateHeader());
    return AdcController.buildError(
        HttpStatus.UNAUTHORIZED, "UMA permissions ticket emitted", headers);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<HttpError> statusException(ResponseStatusException e) {
    Logger.debug("Stacktrace: ", e);
    return AdcController.buildError(e.getStatus(), e.getReason());
  }

  @ExceptionHandler(UmaFlowException.class)
  public ResponseEntity<HttpError> umaFlowHandler(Exception e) {
    Logger.info("Uma flow access error {}", e.getMessage());
    Logger.debug("Stacktrace: ", e);

    return AdcController.buildError(HttpStatus.UNAUTHORIZED, null);
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<HttpError> internalErrorHandler(Exception e) {
    Logger.error("Internal error occurred: ", e);
    return AdcController.buildError(HttpStatus.UNAUTHORIZED, null);
  }

  @RequestMapping(
      value = "/repertoire/{repertoireId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public String repertoire(HttpServletRequest request, @PathVariable String repertoireId)
      throws Exception {
    var umaId = this.dbRepository.getRepertoireUmaId(repertoireId);
    var scopes = this.csvConfig.getUmaScopes(FieldClass.REPERTOIRE);

    exactUmaFlow(request, umaId, "non-existing repertoire in cache " + repertoireId, scopes);

    return this.adcClient.getRepertoireAsString(repertoireId);
  }

  @RequestMapping(
      value = "/rearrangement/{rearrangementId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public String rearrangement(HttpServletRequest request, @PathVariable String rearrangementId)
      throws Exception {
    var umaId = this.dbRepository.getRearrangementUmaId(rearrangementId);
    var scopes = this.csvConfig.getUmaScopes(FieldClass.REARRANGEMENT);

    exactUmaFlow(request, umaId, "non-existing rearrangement in cache " + rearrangementId, scopes);

    return this.adcClient.getRearrangementAsString(rearrangementId);
  }

  @RequestMapping(
      value = "/repertoire",
      method = RequestMethod.GET,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> repertoire_search(
      HttpServletRequest request, @RequestBody AdcSearchRequest adcSearch) throws Exception {
    AdcController.validateAdcSearch(adcSearch);

    var bearer = AdcController.getBearer(request);
    if (bearer == null) {
      var idsQuery = adcSearch.queryClone().addFields(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);
      var umaIds =
          this.adcClient.getRepertoireIds(idsQuery)
              .stream()
              .map(e -> this.dbRepository.getStudyUmaId(e.getStudyId()));
      this.throwNoRptToken(umaIds, FieldClass.REPERTOIRE);
    }

    var isAddedField = adcSearch.tryAddField(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);
    Set<String> removeFields = isAddedField ? ImmutableSet.of(AdcConstants.REPERTOIRE_STUDY_ID_FIELD) : ImmutableSet.of();
    var tokenResources = this.umaClient.introspectToken(bearer);
    var repertoireMapper = this.buildUmaFieldMapper(tokenResources, FieldClass.REPERTOIRE, removeFields)
        .compose(this.dbRepository::getStudyUmaId);

    var response = this.adcClient.searchRepertoiresAsStream(adcSearch);
    var mapper = new ResourceJsonMapper(response, "Repertoire", repertoireMapper, AdcConstants.REPERTOIRE_STUDY_ID_FIELD);

    return AdcController.buildJsonStream(mapper);
  }

  // TODO add security

  @RequestMapping(value = "/synchronize", method = RequestMethod.POST)
  public void synchronize() throws Exception {
    this.dbRepository.synchronize();
  }

  private void exactUmaFlow(
      HttpServletRequest request, String umaId, String errorMsg, Set<String> umaScopes)
      throws Exception {
    Preconditions.checkArgument(umaScopes.size() > 0);

    if (umaId == null) {
      Logger.info("User tried accessing non-existing resource {}", errorMsg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }

    var bearer = AdcController.getBearer(request);
    var umaResource = new UmaResource(umaId, umaScopes);
    this.umaFlow.exactMatchFlow(bearer, umaResource);
  }

  private static void validateAdcSearch(AdcSearchRequest adcSearch) {
    if (adcSearch.getFields() != null && adcSearch.getFacets() != null) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Can't use 'fields' and 'facets' at the same time in request");
    }

    if (!adcSearch.isJsonFormat() || adcSearch.getFacets() != null) {
      Logger.error("Not implemented");
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Not implemented yet");
    }
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

  private void throwNoRptToken(Stream<String> umaIds, FieldClass umaFieldsClass) throws Exception {
    var umaResources = umaIds.filter(Objects::nonNull)
        .collect(Collectors.toSet())
        .stream()
        .map(id -> new UmaResource(id, this.csvConfig.getUmaScopes(umaFieldsClass)))
        .toArray(UmaResource[]::new);

    this.umaFlow.noRptToken(umaResources); // will throw
  }

  private static ResponseEntity<HttpError> buildError(HttpStatus status, String msg) {
    return new ResponseEntity<>(new HttpError(status.value(), msg), status);
  }

  private static ResponseEntity<HttpError> buildError(
      HttpStatus status, String msg, Map<String, String> headers) {
    var responseHeaders = new HttpHeaders();
    headers.forEach(responseHeaders::set);

    return new ResponseEntity<>(new HttpError(status.value(), msg), responseHeaders, status);
  }

  private static ResponseEntity<StreamingResponseBody> buildJsonStream(StreamingResponseBody streamer) {
    return ResponseEntity
        .ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(streamer);
  }

  private static Set<String> EmptySet = ImmutableSet.of();

  private Function<String, Set<String>> buildUmaFieldMapper(List<UmaResource> resources, FieldClass fieldClass, Set<String> diffFields) {
    var validUmaFields = resources.stream()
        .map(uma -> {
          var scopes = uma.getScopes()
              .stream()
              .map(AccessScope::fromString) // can throw
              .collect(Collectors.toSet());

          var fields = this.csvConfig.getFields(FieldClass.REPERTOIRE, scopes);
          return Pair.of(uma.getUmaResourceId(), fields);
        }).collect(Collectors.toMap(Pair::getFirst,Pair::getSecond));

    var publicFields = this.csvConfig.getFields(fieldClass, ImmutableSet.of(AccessScope.PUBLIC));

    return umaId -> {
      if (umaId == null) {
        return EmptySet;
      }

      var fields = validUmaFields.getOrDefault(umaId, EmptySet);
      fields = Sets.union(fields, publicFields);
      return Sets.difference(fields, diffFields);
    };
  }

}
