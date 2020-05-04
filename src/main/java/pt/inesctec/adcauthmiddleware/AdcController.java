package pt.inesctec.adcauthmiddleware;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
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
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class AdcController {
  private static Set<String> EmptySet = ImmutableSet.of();
  private static List<UmaResource> EmptyResources = ImmutableList.of();

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
  public ResponseEntity<StreamingResponseBody> repertoire(HttpServletRequest request, @PathVariable String repertoireId)
      throws Exception {
    var umaId = this.dbRepository.getRepertoireUmaId(repertoireId);
    if (umaId == null) {
      Logger.info("User tried accessing non-existing repertoire with ID {}", repertoireId);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }

    var repertoireMapper = this.umaSingleTokenFlow(request, umaId, FieldClass.REPERTOIRE, this.dbRepository::getStudyUmaId); // can throw
    var response = this.adcClient.getRepertoireAsStream(repertoireId);
    return buildRepertoireMappedJsonStream(repertoireMapper, response);
  }

  @RequestMapping(
      value = "/rearrangement/{rearrangementId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> rearrangement(HttpServletRequest request, @PathVariable String rearrangementId)
      throws Exception {
    var umaId = this.dbRepository.getRearrangementUmaId(rearrangementId);
    if (umaId == null) {
      Logger.info("User tried accessing non-existing rearrangement with ID {}", rearrangementId);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }

    var fieldMapper = this.umaSingleTokenFlow(request, umaId, FieldClass.REARRANGEMENT, this.dbRepository::getRepertoireUmaId); // can throw
    var response = this.adcClient.getRearrangementAsStream(rearrangementId);
    return buildRearrangementMappedJsonStream(fieldMapper, response);
  }

  @RequestMapping(
      value = "/repertoire",
      method = RequestMethod.GET,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> repertoire_search(
      HttpServletRequest request, @RequestBody AdcSearchRequest adcSearch) throws Exception {
    this.validateAdcSearch(adcSearch, FieldClass.REPERTOIRE);

    return searchRepertoiresEndpoint(request, adcSearch);
  }

  @RequestMapping(value = "/synchronize", method = RequestMethod.POST) // TODO add security
  public void synchronize() throws Exception {
    this.dbRepository.synchronize();
  }

  private ResponseEntity<StreamingResponseBody> searchRepertoiresEndpoint(
      HttpServletRequest request, @RequestBody AdcSearchRequest adcSearch) throws Exception {
    var bearer = AdcController.getBearer(request);
    if (bearer == null) {
      var idsQuery = adcSearch.queryClone().addFields(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);
      var umaIds =
          this.adcClient.getRepertoireIds(idsQuery).stream()
              .map(e -> this.dbRepository.getStudyUmaId(e.getStudyId()));
      var umaScopes =
          adcSearch.isFieldsEmpty()
              ? this.csvConfig.getUmaScopes(FieldClass.REPERTOIRE)
              : this.csvConfig.getUmaScopes(FieldClass.REPERTOIRE, adcSearch.getFields());
      if (umaScopes.isEmpty()) { // means only public access fields are requested with the 'fields'
        return this.searchRepertoires(adcSearch, EmptyResources);
      } else {
        this.throwNoRptToken(umaIds, umaScopes);
      }
    }

    var tokenResources = this.umaClient.introspectToken(bearer);
    return this.searchRepertoires(adcSearch, tokenResources);
  }

  private Function<String, Set<String>> umaSingleTokenFlow(HttpServletRequest request, String umaId, FieldClass fieldClass, Function<String, String> adcIdToUmaMapper) throws Exception {
    var bearer = AdcController.getBearer(request);
    if (bearer == null) {
      var umaScopes = this.csvConfig.getUmaScopes(fieldClass);
      this.throwNoRptToken(umaId, umaScopes);
    }

    var tokenResources = this.umaClient.introspectToken(bearer);
    return this.buildUmaFieldMapper(tokenResources, fieldClass, EmptySet)
        .compose(adcIdToUmaMapper);
  }

  private ResponseEntity<StreamingResponseBody> searchRepertoires(
      AdcSearchRequest adcSearch, List<UmaResource> umaResources) throws Exception {
    var isAddedField = adcSearch.tryAddField(AdcConstants.REPERTOIRE_STUDY_ID_FIELD);
    Set<String> removeFields =
        isAddedField ? ImmutableSet.of(AdcConstants.REPERTOIRE_STUDY_ID_FIELD) : EmptySet;
    var repertoireMapper =
        this.buildUmaFieldMapper(umaResources, FieldClass.REPERTOIRE, removeFields)
            .compose(this.dbRepository::getStudyUmaId);

    var response = this.adcClient.searchRepertoiresAsStream(adcSearch);
    return buildRepertoireMappedJsonStream(repertoireMapper, response);
  }

  private void validateAdcSearch(AdcSearchRequest adcSearch, FieldClass fieldClass) {
    var fields = adcSearch.getFields();
    if (fields != null && adcSearch.getFacets() != null) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Can't use 'fields' and 'facets' at the same time in request");
    }

    if (!adcSearch.isJsonFormat() || adcSearch.getFacets() != null) {
      Logger.error("Not implemented");
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Not implemented yet");
    }

    if (fields != null && !fields.isEmpty()) {
      var validFields = this.csvConfig.getFields(fieldClass);
      for (var field : fields) {
        if (!validFields.contains(field)) {
          throw new ResponseStatusException(
              HttpStatus.UNPROCESSABLE_ENTITY, "'fields' '" + field + "' value is not valid");
        }
      }
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

  private void throwNoRptToken(Stream<String> umaIds, Set<String> umaScopes) throws Exception {
    var umaResources =
        umaIds.filter(Objects::nonNull).collect(Collectors.toSet()).stream()
            .map(id -> new UmaResource(id, umaScopes))
            .toArray(UmaResource[]::new);

    this.umaFlow.noRptToken(umaResources); // will throw
  }

  private void throwNoRptToken(String umaId, Set<String> umaScopes) throws Exception {
    var stream = ImmutableList.of(umaId).stream();
    this.throwNoRptToken(stream, umaScopes);
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



  private Function<String, Set<String>> buildUmaFieldMapper(
      List<UmaResource> resources, FieldClass fieldClass, Set<String> diffFields) {
    var validUmaFields =
        resources.stream()
            .map(
                uma -> {
                  var scopes =
                      uma.getScopes().stream()
                          .map(AccessScope::fromString) // can throw
                          .collect(Collectors.toSet());

                  var fields = this.csvConfig.getFields(fieldClass, scopes);
                  return Pair.of(uma.getUmaResourceId(), fields);
                })
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

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

  private static ResponseEntity<StreamingResponseBody> buildJsonStream(
      StreamingResponseBody streamer) {
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(streamer);
  }

  private ResponseEntity<StreamingResponseBody> buildRepertoireMappedJsonStream(Function<String, Set<String>> fieldMapper, InputStream response) {
    var mapper =
        new ResourceJsonMapper(
            response, "Repertoire", fieldMapper, AdcConstants.REPERTOIRE_STUDY_ID_FIELD);

    return AdcController.buildJsonStream(mapper);
  }

  private ResponseEntity<StreamingResponseBody> buildRearrangementMappedJsonStream(Function<String, Set<String>> fieldMapper, InputStream response) {
    var mapper =
        new ResourceJsonMapper(
            response, "Rearrangement", fieldMapper, AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD);

    return AdcController.buildJsonStream(mapper);
  }
}
