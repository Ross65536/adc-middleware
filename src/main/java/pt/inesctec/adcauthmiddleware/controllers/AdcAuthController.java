package pt.inesctec.adcauthmiddleware.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.HttpException;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.AdcJsonDocumentParser;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.FieldsFilter;
import pt.inesctec.adcauthmiddleware.adc.resourceprocessing.IFieldsFilter;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.db.DbRepository;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.exceptions.UmaFlowException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;
import pt.inesctec.adcauthmiddleware.utils.Delayer;
import pt.inesctec.adcauthmiddleware.utils.ThrowingFunction;
import pt.inesctec.adcauthmiddleware.utils.ThrowingProducer;

/**
 * class responsible for the protected endpoints.
 */
@RestController
public class AdcAuthController {
  private static Set<String> EmptySet = ImmutableSet.of();
  private static List<UmaResource> EmptyResources = ImmutableList.of();
  private static org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcAuthController.class);
  private static final PasswordEncoder PasswordEncoder = new BCryptPasswordEncoder();

  private AppConfig appConfig;
  private final Delayer repertoiresDelayer;
  private final Delayer rearrangementsDelayer;
  @Autowired private AdcClient adcClient;
  @Autowired private DbRepository dbRepository;
  @Autowired private UmaFlow umaFlow;
  @Autowired private UmaClient umaClient;
  @Autowired private CsvConfig csvConfig;

  @Autowired
  public AdcAuthController(AppConfig appConfig) {
    this.appConfig = appConfig;

    this.repertoiresDelayer = new Delayer(appConfig.getRequestDelaysPoolSize());
    this.rearrangementsDelayer = new Delayer(appConfig.getRequestDelaysPoolSize());
  }

  private static final Pattern JsonErrorPattern =
      Pattern.compile(".*line: (\\d+), column: (\\d+).*");

  /**
   * Handles and logs errors on invalid user JSON body (on POST endpoints) such as invalid syntax or some invalid schema.
   * @param e exception
   * @return error message
   */
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

  /**
   * Used when an application wants to return a specific error code.
   * @param e exception
   * @return error code + message
   */
  @ExceptionHandler(HttpException.class)
  public ResponseEntity<String> httpExceptionForward(HttpException e) {
    Logger.debug("Stacktrace: ", e);
    return SpringUtils.buildResponse(e.statusCode, e.errorMsg, e.contentType.orElse(null));
  }

  /**
   * Not actually a logic error. Returns the UMA permissions ticket to the user.
   * @param e exception
   * @return 401 + permissions ticket
   */
  @ExceptionHandler(TicketException.class)
  public ResponseEntity<String> ticketHandler(TicketException e) {
    var headers = ImmutableMap.of(HttpHeaders.WWW_AUTHENTICATE, e.buildAuthenticateHeader());
    return SpringUtils.buildJsonErrorResponse(
        HttpStatus.UNAUTHORIZED, "UMA permissions ticket emitted", headers);
  }

  /**
   * Logs errors related to the UMA flow (UMA client and UMA flow).
   * @param e Exception
   * @return 401 status code
   */
  @ExceptionHandler(UmaFlowException.class)
  public ResponseEntity<String> umaFlowHandler(Exception e) {
    Logger.info("Uma flow access error {}", e.getMessage());
    Logger.debug("Stacktrace: ", e);

    return SpringUtils.buildJsonErrorResponse(HttpStatus.UNAUTHORIZED, null);
  }

  /**
   * Logs synchronization endpoint user errors.
   * @param e exception
   * @return 401 status code
   */
  @ExceptionHandler(SyncException.class)
  public ResponseEntity<String> synchronizeErrorHandler(SyncException e) {
    Logger.info("Synchronize: {}", e.getMessage());
    return SpringUtils.buildJsonErrorResponse(HttpStatus.UNAUTHORIZED, null);
  }

  /**
   * Logs unhandled internal exceptions. Errors here can indicate a logic error or bug.
   * @param e exception
   * @return 401 status code
   */
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<String> internalErrorHandler(Exception e) {
    Logger.error("Internal error occurred: {}", e.getMessage());
    Logger.debug("Stacktrace: ", e);
    return SpringUtils.buildJsonErrorResponse(HttpStatus.UNAUTHORIZED, null);
  }

  /**
   * Protected by UMA. Individual repertoire. Part of ADC v1.
   * @param request user request
   * @param repertoireId repertoire ID
   * @return the filtered repertoire
   * @throws Exception if user does not have permissions or some other error occurs
   */
  @RequestMapping(
      value = "/repertoire/{repertoireId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> repertoire(
      HttpServletRequest request, @PathVariable String repertoireId) throws Exception {
    var bearer = SpringUtils.getBearer(request);
    if (bearer == null) {
      var umaId = this.dbRepository.getRepertoireUmaId(repertoireId);
      if (umaId == null) {
        Logger.info("User tried accessing non-existing repertoire with ID {}", repertoireId);
        throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
      }

      var umaScopes = this.csvConfig.getUmaScopes(FieldClass.REPERTOIRE);
      throw this.umaFlow.noRptToken(ImmutableList.of(umaId), umaScopes);
    }

    var tokenResources = this.umaClient.introspectToken(bearer);
    var fieldMapper =
        this.buildUmaFieldMapper(tokenResources, FieldClass.REPERTOIRE)
            .compose(this.dbRepository::getStudyUmaId);

    return buildFilteredJsonResponse(
        AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
        AdcConstants.REPERTOIRE_RESPONSE_FILTER_FIELD,
        fieldMapper,
        () -> this.adcClient.getRepertoireAsStream(repertoireId));
  }

  /**
   * Protected by UMA. Individual rearrangement. Part of ADC v1.
   * @param request user request
   * @param rearrangementId rearrangement ID
   * @return the filtered rearrangement
   * @throws Exception if user does not have permissions or some other error occurs
   */
  @RequestMapping(
      value = "/rearrangement/{rearrangementId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> rearrangement(
      HttpServletRequest request, @PathVariable String rearrangementId) throws Exception {

    var bearer = SpringUtils.getBearer(request);
    if (bearer == null) {
      String umaId = this.dbRepository.getRearrangementUmaId(rearrangementId);
      if (umaId == null) {
        Logger.info("User tried accessing non-existing rearrangement with ID {}", rearrangementId);
        throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
      }

      var umaScopes = this.csvConfig.getUmaScopes(FieldClass.REARRANGEMENT);
      throw this.umaFlow.noRptToken(ImmutableList.of(umaId), umaScopes);
    }

    var tokenResources = this.umaClient.introspectToken(bearer);
    var fieldMapper =
        this.buildUmaFieldMapper(tokenResources, FieldClass.REARRANGEMENT)
            .compose(this.dbRepository::getRepertoireUmaId);

    return buildFilteredJsonResponse(
        AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD,
        AdcConstants.REARRANGEMENT_RESPONSE_FILTER_FIELD,
        fieldMapper,
        () -> this.adcClient.getRearrangementAsStream(rearrangementId));
  }

  /**
   * Protected by UMA. Repertoires search. Part of ADC v1.
   * JSON processed in streaming mode. Can return resource public fields if not given access to a resource.
   *
   * @param request user request
   * @return the filtered repertoires stream
   * @throws Exception if some error occurs
   */
  @RequestMapping(
      value = "/repertoire",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> repertoireSearch(
      HttpServletRequest request, @RequestBody AdcSearchRequest adcSearch) throws Exception {
    this.validateAdcSearch(adcSearch, FieldClass.REPERTOIRE, false);

    Set<String> umaScopes = this.getAdcRequestUmaScopes(adcSearch, FieldClass.REPERTOIRE);
    var umaResources =
        this.adcQueryUmaFlow(
            request, adcSearch, this::getRepertoireStudyIds, repertoiresDelayer, umaScopes);

    if (adcSearch.isFacetsSearch()) {
      final List<String> resourceIds =
          calcValidFacetsResources(
              umaResources,
              umaScopes,
              (umaId) -> CollectionsUtils.toSet(this.dbRepository.getUmaStudyId(umaId)));

      return facetsRequest(
          adcSearch,
          AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
          this.adcClient::searchRepertoiresAsStream,
          resourceIds,
          !umaScopes.isEmpty());
    }

    var fieldMapper =
        this.adcRegularSearchSetup(
            adcSearch, AdcConstants.REPERTOIRE_STUDY_ID_FIELD, FieldClass.REPERTOIRE, umaResources);
    return buildFilteredJsonResponse(
        AdcConstants.REPERTOIRE_STUDY_ID_FIELD,
        AdcConstants.REPERTOIRE_RESPONSE_FILTER_FIELD,
        fieldMapper.compose(this.dbRepository::getStudyUmaId),
        () -> this.adcClient.searchRepertoiresAsStream(adcSearch));
  }

  /**
   * Protected by UMA. Rearrangements search. Part of ADC v1.
   * JSON processed in streaming mode. Can return resource public fields if not given access to a resource.
   *
   * @param request user request
   * @return the filtered rearrangements stream
   * @throws Exception if some error occurs
   */
  @RequestMapping(
      value = "/rearrangement",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StreamingResponseBody> rearrangementSearch(
      HttpServletRequest request, @RequestBody AdcSearchRequest adcSearch) throws Exception {
    this.validateAdcSearch(adcSearch, FieldClass.REARRANGEMENT, true);
    final boolean isJsonFormat = adcSearch.isJsonFormat();
    adcSearch.unsetFormat();

    Set<String> umaScopes = this.getAdcRequestUmaScopes(adcSearch, FieldClass.REARRANGEMENT);
    var umaResources =
        this.adcQueryUmaFlow(
            request,
            adcSearch,
            this::getRearrangementsRepertoireIds,
            rearrangementsDelayer,
            umaScopes);

    if (adcSearch.isFacetsSearch()) {
      final List<String> resourceIds =
          calcValidFacetsResources(umaResources, umaScopes, this.dbRepository::getUmaRepertoireIds);

      return facetsRequest(
          adcSearch,
          AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD,
          this.adcClient::searchRearrangementsAsStream,
          resourceIds,
          !umaScopes.isEmpty());
    }

    var fieldMapper =
        this.adcRegularSearchSetup(
            adcSearch,
            AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD,
            FieldClass.REARRANGEMENT,
            umaResources);

    if (isJsonFormat) {
      return buildFilteredJsonResponse(
          AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD,
          AdcConstants.REARRANGEMENT_RESPONSE_FILTER_FIELD,
          fieldMapper.compose(this.dbRepository::getRepertoireUmaId),
          () -> this.adcClient.searchRearrangementsAsStream(adcSearch));
    }

    var requestedFieldTypes = getRegularSearchRequestedFieldsAndTypes(adcSearch, FieldClass.REARRANGEMENT);
    return buildFilteredTsvResponse(
        AdcConstants.REARRANGEMENT_REPERTOIRE_ID_FIELD,
        AdcConstants.REARRANGEMENT_RESPONSE_FILTER_FIELD,
        fieldMapper.compose(this.dbRepository::getRepertoireUmaId),
        () -> this.adcClient.searchRearrangementsAsStream(adcSearch),
        requestedFieldTypes);
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
   * The synchronize endpoint. Not part of ADC v1. Protected by password set in the configuration file. Extension of the middleware.
   * Performs state synchronization between the repository and the UMA authorization server and this middleware's DB.
   * Resets the delays pool request times.
   *
   * @param request the user request
   * @return OK on successfull synchronization or an error code when a process(es) in the synchronization fails.
   * @throws Exception on user errors such as invalid password or some internal errors.
   */
  @RequestMapping(value = "/synchronize", method = RequestMethod.POST)
  public Map<String, Object> synchronize(HttpServletRequest request) throws Exception {
    String bearer = SpringUtils.getBearer(request);
    if (bearer == null) {
      throw new SyncException("Invalid user credential format");
    }

    if (!PasswordEncoder.matches(bearer, appConfig.getSynchronizePasswordHash())) {
      throw new SyncException("Invalid user credential");
    }

    if (!this.dbRepository.synchronize()) {
      throw SpringUtils.buildHttpException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "One or more DB or UMA resources failed to synchronize, check logs");
    }

    this.rearrangementsDelayer.reset();
    this.repertoiresDelayer.reset();

    return SpringUtils.buildStatusMessage(200, null);
  }

  /**
   * Function to obtain the unique study UMA IDs that correspond to the user's repertoire ADC query search.
   * @param idsQuery the ADC query
   * @return the UMA IDs
   * @throws Exception on error
   */
  private Set<String> getRearrangementsRepertoireIds(AdcSearchRequest idsQuery) throws Exception {
    return this.adcClient.getRearrangementRepertoireIds(idsQuery).stream()
        .map(id -> this.dbRepository.getRepertoireUmaId(id))
        .collect(Collectors.toSet());
  }

  /**
   * Function to obtain the unique study UMA IDs that correspond to the user's rearrangement ADC query search.
   * @param idsQuery the ADC query
   * @return the UMA IDs
   * @throws Exception on error
   */
  private Set<String> getRepertoireStudyIds(AdcSearchRequest idsQuery) throws Exception {
    return this.adcClient.getRepertoireStudyIds(idsQuery).stream()
        .map(id -> this.dbRepository.getStudyUmaId(id))
        .collect(Collectors.toSet());
  }

  /**
   * The common UMA flow for POST endpoints. Emits a permissions ticket or returns the introspected RPT token resources.
   * @param request the user request
   * @param adcSearch the user ADC query
   * @param umaIdsProducer the producer that will return the resources matching the user query
   * @param delayer the delayer to make all requests take the same time.
   * @param umaScopes the scopes set for the request (for emitting permissions ticket).
   * @return the introspected RPT resources.
   * @throws Exception when emitting a permission ticket or an internal error occurs.
   */
  private List<UmaResource> adcQueryUmaFlow(
      HttpServletRequest request,
      AdcSearchRequest adcSearch,
      ThrowingFunction<AdcSearchRequest, Collection<String>, Exception> umaIdsProducer,
      Delayer delayer,
      Set<String> umaScopes)
      throws Exception {
    var startTime = LocalDateTime.now();

    // empty scopes means public access, no UMA flow followed
    if (umaScopes.isEmpty()) {
      return EmptyResources;
    }

    var bearer = SpringUtils.getBearer(request);
    if (bearer != null) {
      return this.umaClient.introspectToken(bearer);
    }

    Collection<String> umaIds = umaIdsProducer.apply(adcSearch);
    delayer.delay(startTime);

    if (umaIds.isEmpty()) {
      // when no resources return, just err
      throw SpringUtils.buildHttpException(HttpStatus.UNAUTHORIZED, null);
    }

    throw this.umaFlow.noRptToken(umaIds, umaScopes);
  }

  /**
   * Returns the UMA scopes for the fields that are being requested in the ADC query. The considered parameters are: "facets", "fields", "include_fields", and "filters". Filters operators can reference a field for the search and these are the fields considered.
   *
   * @param adcSearch the ADC query
   * @param fieldClass the resource type
   * @return the UMA scopes.
   */
  private Set<String> getAdcRequestUmaScopes(AdcSearchRequest adcSearch, FieldClass fieldClass) {
    final Set<String> requestedFields =
        adcSearch.isFacetsSearch()
            ? Set.of(adcSearch.getFacets())
            : getRegularSearchRequestedFields(adcSearch, fieldClass);
    final Set<String> filtersFields = adcSearch.getFiltersFields();
    final Set<String> allConsideredFields = Sets.union(requestedFields, filtersFields);

    // empty set returned means only public fields requested
    return this.csvConfig.getUmaScopes(fieldClass, allConsideredFields);
  }

  /**
   * Core facets request.
   *
   * @param adcSearch the user's ADC query.
   * @param resourceId the resource's ID field
   * @param adcRequest the request function
   * @param resourceIds the permitted list of resource IDs for facets.
   * @param restrictedAccess wheter the request made is protected or public.
   * @return the streamed facets.
   * @throws Exception on error.
   */
  private ResponseEntity<StreamingResponseBody> facetsRequest(
      AdcSearchRequest adcSearch,
      String resourceId,
      ThrowingFunction<AdcSearchRequest, InputStream, Exception> adcRequest,
      List<String> resourceIds,
      boolean restrictedAccess)
      throws Exception {

    boolean filterResponse = false;
    if (restrictedAccess) { // non public facets field
      adcSearch.withFieldIn(resourceId, resourceIds);
      filterResponse = resourceIds.isEmpty();
    }

    var is = SpringUtils.catchForwardingError(() -> adcRequest.apply(adcSearch));
    // will only perform whitelist filtering if rpt grants access to nothing, for partial access the
    // backend must perform the filtering
    IFieldsFilter filter = filterResponse ? FieldsFilter.BlockingFilter : FieldsFilter.OpenFilter;
    var mapper = AdcJsonDocumentParser.buildJsonMapper(is, AdcConstants.ADC_FACETS, filter);
    return SpringUtils.buildJsonStream(mapper);
  }

  /**
   * From the UMA resource list and scopes obtain the list of resource IDs that can be safely processed for the resource type.
   *
   * @param umaResources the UMA resources and scopes.
   * @param umaScopes the UMA scopes that the user must have access to for the resource, otherwise the resource is not considered.
   * @param umaIdGetter function that returns the collection of resource IDs given the UMA ID.
   * @return the filtered collection of resource IDs.
   */
  private List<String> calcValidFacetsResources(
      Collection<UmaResource> umaResources,
      Set<String> umaScopes,
      Function<String, Set<String>> umaIdGetter) {
    return umaResources.stream()
        .filter(resource -> !Sets.intersection(umaScopes, resource.getScopes()).isEmpty())
        .map(resource -> umaIdGetter.apply(resource.getUmaResourceId()))
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Setup the ADC request and build the mapper for the regular search.
   *
   * @param adcSearch the user's ADC query. Can be modified by reference.
   * @param resourceId the resource's ID field.
   * @param fieldClass the resource type.
   * @param umaResources the UMA resources and scopes.
   * @return the UMA ID to permitted fields mapper
   */
  private Function<String, Set<String>> adcRegularSearchSetup(
      AdcSearchRequest adcSearch, // will be modified by reference
      String resourceId,
      FieldClass fieldClass,
      Collection<UmaResource> umaResources) {
    final Set<String> allRequestedFields = getRegularSearchRequestedFields(adcSearch, fieldClass);
    final Set<String> filtersFields = adcSearch.getFiltersFields();

    if (!allRequestedFields.contains(resourceId)) {
      adcSearch.addField(resourceId);
    }

    return this.buildUmaFieldMapper(umaResources, fieldClass)
        .andThen(
            fields -> {
              // don't return resources where the access level does not match the one in the
              // filters, in order to avoid information leaks
              if (Sets.difference(filtersFields, fields).isEmpty()) {
                return fields;
              }

              return EmptySet;
            })
        .andThen(set -> Sets.intersection(set, allRequestedFields));
  }

  /**
   * Validate that the user's ADC query is semantically correct. Also enforces disabled features as set in the configuration.
   * @param adcSearch the user's ADC query
   * @param fieldClass the resource type
   * @param tsvEnabled whether TSV is enabled for the considered endpoint.
   * @throws HttpException on validation error
   */
  private void validateAdcSearch(
      AdcSearchRequest adcSearch, FieldClass fieldClass, boolean tsvEnabled) throws HttpException {

    if (adcSearch.isFacetsSearch() && !this.appConfig.isFacetsEnabled()) {
      throw SpringUtils.buildHttpException(
          HttpStatus.NOT_IMPLEMENTED,
          "Invalid input JSON: 'facets' support for current repository not enabled");
    }

    if (adcSearch.getFilters() != null && !this.appConfig.isAdcFiltersEnabled()) {
      throw SpringUtils.buildHttpException(
          HttpStatus.NOT_IMPLEMENTED,
          "Invalid input JSON: 'filters' support for current repository not enabled");
    }

    var filtersBlacklist = this.appConfig.getFiltersOperatorsBlacklist();
    Set<String> actualFiltersOperators = adcSearch.getFiltersOperators();
    Sets.SetView<String> operatorDiff = Sets.intersection(filtersBlacklist, actualFiltersOperators);
    if (!operatorDiff.isEmpty()) {
      throw SpringUtils.buildHttpException(
          HttpStatus.NOT_IMPLEMENTED,
          "Invalid input JSON: 'filters' operators: "
              + CollectionsUtils.toString(operatorDiff)
              + " are blacklisted");
    }

    final boolean isTsv = !adcSearch.isJsonFormat();
    if (isTsv && !tsvEnabled) {
      throw SpringUtils.buildHttpException(
          HttpStatus.UNPROCESSABLE_ENTITY, "TSV format not enabled for this endpoint");
    }

    var fieldTypes = this.csvConfig.getFieldsAndTypes(fieldClass);
    var requestedFields = getRegularSearchRequestedFields(adcSearch, FieldClass.REARRANGEMENT);
    try {
      AdcSearchRequest.validate(adcSearch, fieldTypes, requestedFields);
    } catch (AdcException e) {
      throw SpringUtils.buildHttpException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Invalid input JSON: " + e.getMessage());
    }
  }

  /**
   * Build mapper function from UMA ID to the permitted fields for each resource for the user, given by the UMA resource list.
   * If access is not granted for a resource the public fields for the resource type are returned, if there are any.
   * Used for non-facets regular searches or individual endpoints.
   *
   * @param resources the UMA resource list with their scopes
   * @param fieldClass the resource type
   * @return the mapper function
   */
  private Function<String, Set<String>> buildUmaFieldMapper(
      Collection<UmaResource> resources, FieldClass fieldClass) {
    var validUmaFields =
        resources.stream()
            .collect(
                Collectors.toMap(
                    UmaResource::getUmaResourceId,
                    uma -> this.csvConfig.getFields(fieldClass, uma.getScopes())));

    var publicFields = this.csvConfig.getPublicFields(fieldClass);

    return umaId -> {
      if (umaId == null) {
        Logger.warn(
            "A resource was returned by the repository with no mapping from resource ID to UMA ID. Consider synchronizing.");
        return publicFields;
      }

      var fields = validUmaFields.getOrDefault(umaId, EmptySet);
      return Sets.union(fields, publicFields);
    };
  }

  /**
   * Build JSON streaming, filtered response.
   *
   * @param resourceId the resource's ID fields
   * @param responseFilterField the response's field where the resources are set
   * @param fieldMapper the ID to granted fields mapper
   * @param adcRequest the ADC request producer.
   * @return streaming response
   * @throws Exception on error
   */
  private static ResponseEntity<StreamingResponseBody> buildFilteredJsonResponse(
      String resourceId,
      String responseFilterField,
      Function<String, Set<String>> fieldMapper,
      ThrowingProducer<InputStream, Exception> adcRequest)
      throws Exception {
    var response = SpringUtils.catchForwardingError(adcRequest);
    var filter = new FieldsFilter(fieldMapper, resourceId);
    var mapper = AdcJsonDocumentParser.buildJsonMapper(response, responseFilterField, filter);
    return SpringUtils.buildJsonStream(mapper);
  }

  /**
   * Build TSV streaming, filtered, response
   * @param resourceId the resource's ID fields
   * @param responseFilterField the response's field where the resources are set
   * @param fieldMapper the ID to granted fields mapper
   * @param adcRequest the ADC request producer.
   * @param headerFields the TSV header fields which will be the response's first line.
   * @return streaming response
   * @throws Exception on error
   */
  private ResponseEntity<StreamingResponseBody> buildFilteredTsvResponse(
      String resourceId,
      String responseFilterField,
      Function<String, Set<String>> fieldMapper,
      ThrowingProducer<InputStream, Exception> adcRequest,
      Map<String, FieldType> headerFields)
      throws Exception {
    var response = SpringUtils.catchForwardingError(adcRequest);
    var filter = new FieldsFilter(fieldMapper, resourceId);
    var mapper =
        AdcJsonDocumentParser.buildTsvMapper(response, responseFilterField, filter, headerFields);
    return SpringUtils.buildTsvStream(mapper);
  }

  /**
   * Get the fields that correspond to the ADC query, non-facets. Facets presence should be checked previously.
   * Only the "fields" and "include_fields" parameters are considered. If both empty all of the resource's fields are returned.
   *
   * @param adcSearch the user's ADC query
   * @param fieldClass the resource type
   * @return the set of fields that were requested.
   */
  private Set<String> getRegularSearchRequestedFields(
      AdcSearchRequest adcSearch, FieldClass fieldClass) {
    final Set<String> adcFields = adcSearch.isFieldsEmpty() ? Set.of() : adcSearch.getFields();
    final Set<String> adcIncludeFields =
        adcSearch.isIncludeFieldsEmpty()
            ? Set.of()
            : this.csvConfig.getFields(fieldClass, adcSearch.getIncludeFields());
    final Set<String> requestedFields = Sets.union(adcFields, adcIncludeFields);
    return new HashSet<>(
        requestedFields.isEmpty()
            ? this.csvConfig.getFieldsAndTypes(fieldClass).keySet()
            : requestedFields);
  }

  /**
   * Obtain the fields and their types from the user's regular ADC request. Should check previously that the request is not facets.
   *
   * @param request The user's ADC query
   * @param fieldClass the resource type
   * @return the fields and types
   */
  private Map<String, FieldType> getRegularSearchRequestedFieldsAndTypes(
      AdcSearchRequest request, FieldClass fieldClass) {
    var requestedFields = getRegularSearchRequestedFields(request, fieldClass);
    Map<String, FieldType> allFields = this.csvConfig.getFieldsAndTypes(fieldClass);
    return CollectionsUtils.intersectMapWithSet(allFields, requestedFields);
  }
}
