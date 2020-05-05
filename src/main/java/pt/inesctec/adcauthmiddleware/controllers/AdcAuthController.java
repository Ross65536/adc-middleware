package pt.inesctec.adcauthmiddleware.controllers;

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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.HttpException;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.ResourceJsonMapper;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
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
import pt.inesctec.adcauthmiddleware.utils.ThrowingFunction;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class AdcAuthController {
  private static Set<String> EmptySet = ImmutableSet.of();
  private static List<UmaResource> EmptyResources = ImmutableList.of();
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcAuthController.class);

  @Autowired private AdcClient adcClient;
  @Autowired private DbRepository dbRepository;
  @Autowired private UmaFlow umaFlow;
  @Autowired private UmaClient umaClient;
  @Autowired private CsvConfig csvConfig;

  @Autowired
  public AdcAuthController(DbRepository dbRepository) throws Exception {
    //    cacheRepository.synchronize();
  }

  private static final Pattern JsonErrorPattern =
      Pattern.compile(".*line: (\\d+), column: (\\d+).*");

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> badInputHandler(HttpMessageNotReadableException e) {
    Logger.info("User input JSON error: {}", e.getMessage());

    // TODO improve returned error MSG
    var msg = "";
    var matcher = JsonErrorPattern.matcher(e.getMessage());
    if (matcher.find()) {
      msg =
          String.format(
              " (malformed or invalid ADC schema) at line %s, column %s",
              matcher.group(1), matcher.group(2));
    }

    var cause = e.getRootCause();
    if (cause instanceof AdcException) {
      msg += ": " + cause.getMessage();
    }

    return SpringUtils.buildJsonErrorResponse(HttpStatus.BAD_REQUEST, "Invalid input JSON" + msg);
  }

  @ExceptionHandler(HttpException.class)
  public ResponseEntity<String> httpExceptionForward(HttpException e) {
    Logger.debug("Stacktrace: ", e);
    return SpringUtils.buildResponse(e.statusCode, e.errorMsg, e.contentType.orElse(null));
  }

  @ExceptionHandler(TicketException.class)
  public ResponseEntity<String> ticketHandler(TicketException e) {
    var headers = ImmutableMap.of(HttpHeaders.WWW_AUTHENTICATE, e.buildAuthenticateHeader());
    return SpringUtils.buildJsonErrorResponse(
        HttpStatus.UNAUTHORIZED, "UMA permissions ticket emitted", headers);
  }

  @ExceptionHandler(UmaFlowException.class)
  public ResponseEntity<String> umaFlowHandler(Exception e) {
    Logger.info("Uma flow access error {}", e.getMessage());
    Logger.debug("Stacktrace: ", e);

    return SpringUtils.buildJsonErrorResponse(HttpStatus.UNAUTHORIZED, null);
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<String> internalErrorHandler(Exception e) {
    Logger.error("Internal error occurred: {}", e.getMessage());
    Logger.info("Stacktrace: ", e);
    return SpringUtils.buildJsonErrorResponse(HttpStatus.UNAUTHORIZED, null);
  }

  @RequestMapping(
      value = "/repertoire/{repertoireId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> repertoire(
      HttpServletRequest request, @PathVariable String repertoireId) throws Exception {
    var umaId = this.dbRepository.getRepertoireUmaId(repertoireId);
    if (umaId == null) {
      Logger.info("User tried accessing non-existing repertoire with ID {}", repertoireId);
      throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
    }

    var repertoireMapper =
        this.singleRequestFieldMapperFlow(
            request, umaId, FieldClass.REPERTOIRE, this.dbRepository::getStudyUmaId); // can throw
    var response = SpringUtils.catchForwardingError(() -> this.adcClient.getRepertoireAsStream(repertoireId));
    return buildRepertoireMappedJsonStream(repertoireMapper, response);
  }

  @RequestMapping(
      value = "/rearrangement/{rearrangementId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> rearrangement(
      HttpServletRequest request, @PathVariable String rearrangementId) throws Exception {
    var umaId = this.dbRepository.getRearrangementUmaId(rearrangementId);
    if (umaId == null) {
      Logger.info("User tried accessing non-existing rearrangement with ID {}", rearrangementId);
      throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
    }

    var fieldMapper =
        this.singleRequestFieldMapperFlow(
            request,
            umaId,
            FieldClass.REARRANGEMENT,
            this.dbRepository::getRepertoireUmaId); // can throw
    var response = SpringUtils.catchForwardingError(() -> this.adcClient.getRearrangementAsStream(rearrangementId));
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

  @RequestMapping(
      value = "/rearrangement",
      method = RequestMethod.GET,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> rearrangement_search(
      HttpServletRequest request, @RequestBody AdcSearchRequest adcSearch) throws Exception {
    this.validateAdcSearch(adcSearch, FieldClass.REARRANGEMENT);

    return searchRearrangementsEndpoint(request, adcSearch);
  }

  @RequestMapping(value = "/synchronize", method = RequestMethod.POST) // TODO add security
  public void synchronize() throws Exception {
    this.dbRepository.synchronize();
  }

  @RequestMapping(
      value = "/echo",
      method = RequestMethod.GET,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public AdcSearchRequest echo(@RequestBody AdcSearchRequest adcSearch) throws Exception {
    this.validateAdcSearch(adcSearch, FieldClass.REPERTOIRE);

    return adcSearch;
  }

  private Function<String, Set<String>> adcSearchFieldMapperFlow(
      HttpServletRequest request,
      AdcSearchRequest adcSearch, // will be modified
      FieldClass fieldClass,
      String resourceId,
      ThrowingFunction<AdcSearchRequest, Collection<String>, Exception> umaIdsProducer)
      throws Exception {
    List<UmaResource> umaResources = null;

    var bearer = SpringUtils.getBearer(request);
    if (bearer != null) {
      umaResources = this.umaClient.introspectToken(bearer);
    } else {
      var umaScopes =
          adcSearch.isFieldsEmpty()
              ? this.csvConfig.getUmaScopes(fieldClass)
              : this.csvConfig.getUmaScopes(fieldClass, adcSearch.getFields());
      if (!umaScopes.isEmpty()) { // means only public access fields are requested with the 'fields'
        var idsQuery = adcSearch.queryClone().addFields(resourceId);
        Collection<String> umaIds = umaIdsProducer.apply(idsQuery);
        this.throwNoRptToken(umaIds, umaScopes); // throws
      }

      umaResources = EmptyResources; // fall through
    }

    var isAddedField = adcSearch.tryAddField(resourceId); // modifies function argument
    Set<String> removeFields = isAddedField ? ImmutableSet.of(resourceId) : EmptySet;

    return this.buildUmaFieldMapper(umaResources, fieldClass, removeFields);
  }

  private ResponseEntity<StreamingResponseBody> searchRepertoiresEndpoint(
      HttpServletRequest request, AdcSearchRequest adcSearch) throws Exception {

    ThrowingFunction<AdcSearchRequest, Collection<String>, Exception> lazyUmaIds =
        idsQuery ->
            this.adcClient.getRepertoireIds(idsQuery).stream()
                .map(e -> this.dbRepository.getStudyUmaId(e.getStudyId()))
                .collect(Collectors.toList());

    var fieldMapper =
        this.adcSearchFieldMapperFlow(
                request,
                adcSearch,
                FieldClass.REPERTOIRE,
                AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
                lazyUmaIds)
            .compose(this.dbRepository::getStudyUmaId);

    var response = SpringUtils.catchForwardingError(() -> this.adcClient.searchRepertoiresAsStream(adcSearch));
    return buildRepertoireMappedJsonStream(fieldMapper, response);
  }

  private ResponseEntity<StreamingResponseBody> searchRearrangementsEndpoint(
      HttpServletRequest request, AdcSearchRequest adcSearch) throws Exception {
    ThrowingFunction<AdcSearchRequest, Collection<String>, Exception> lazyUmaIds =
        idsQuery ->
            this.adcClient.getRearrangementIds(idsQuery).stream()
                .map(e -> this.dbRepository.getRepertoireUmaId(e.getRepertoireId()))
                .collect(Collectors.toList());

    var fieldMapper =
        this.adcSearchFieldMapperFlow(
                request,
                adcSearch,
                FieldClass.REARRANGEMENT,
                AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD,
                lazyUmaIds)
            .compose(this.dbRepository::getRepertoireUmaId);

    var response = SpringUtils.catchForwardingError(() -> this.adcClient.searchRearrangementsAsStream(adcSearch));
    return buildRearrangementMappedJsonStream(fieldMapper, response);
  }

  private Function<String, Set<String>> singleRequestFieldMapperFlow(
      HttpServletRequest request,
      String umaId,
      FieldClass fieldClass,
      Function<String, String> adcIdToUmaMapper)
      throws Exception {
    var bearer = SpringUtils.getBearer(request);
    if (bearer == null) {
      var umaScopes = this.csvConfig.getUmaScopes(fieldClass);
      this.throwNoRptToken(ImmutableList.of(umaId), umaScopes);
    }

    var tokenResources = this.umaClient.introspectToken(bearer);
    return this.buildUmaFieldMapper(tokenResources, fieldClass, EmptySet).compose(adcIdToUmaMapper);
  }

  private void validateAdcSearch(AdcSearchRequest adcSearch, FieldClass fieldClass) throws HttpException {
    var fieldTypes = this.csvConfig.getFields(fieldClass);
    try {
      AdcSearchRequest.validate(adcSearch, fieldTypes);
    } catch (AdcException e) {
      throw SpringUtils.buildHttpException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid input JSON: " + e.getMessage());
    }

    if (!adcSearch.isJsonFormat() || adcSearch.getFacets() != null) {
      Logger.error("Not implemented");
      throw SpringUtils.buildHttpException(HttpStatus.UNPROCESSABLE_ENTITY, "Not implemented yet");
    }
  }

  private void throwNoRptToken(Collection<String> umaIds, Set<String> umaScopes) throws Exception {
    var umaResources =
        umaIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())
            .stream()
            .map(id -> new UmaResource(id, umaScopes))
            .toArray(UmaResource[]::new);

    this.umaFlow.noRptToken(umaResources); // will throw
  }

  private Function<String, Set<String>> buildUmaFieldMapper(
      List<UmaResource> resources,
      FieldClass fieldClass,
      Set<String> diffFields) { // TODO extract into class
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

  private static ResponseEntity<StreamingResponseBody> buildRepertoireMappedJsonStream(
      Function<String, Set<String>> fieldMapper, InputStream response) {
    var mapper =
        new ResourceJsonMapper(
            response, "Repertoire", fieldMapper, AdcConstants.REPERTOIRE_STUDY_ID_FIELD);

    return SpringUtils.buildJsonStream(mapper);
  }

  private static ResponseEntity<StreamingResponseBody> buildRearrangementMappedJsonStream(
      Function<String, Set<String>> fieldMapper, InputStream response) {
    var mapper =
        new ResourceJsonMapper(
            response, "Rearrangement", fieldMapper, AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD);

    return SpringUtils.buildJsonStream(mapper);
  }
}
