package pt.inesctec.adcauthmiddleware.controllers;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.HttpException;
import pt.inesctec.adcauthmiddleware.adc.dto.RepertoireDto;
import pt.inesctec.adcauthmiddleware.adc.models.AdcException;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.adc.resources.RearrangementResource;
import pt.inesctec.adcauthmiddleware.adc.resources.RearrangementSet;
import pt.inesctec.adcauthmiddleware.adc.resources.RepertoireResource;
import pt.inesctec.adcauthmiddleware.adc.resources.RepertoireSet;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.db.services.DbService;
import pt.inesctec.adcauthmiddleware.uma.UmaFlow;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.exceptions.UmaFlowException;
import pt.inesctec.adcauthmiddleware.utils.Delayer;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;

/**
 * REST Controller to managing protected endpoints.
 */
@RestController
public class AdcAuthController extends AdcController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcAuthController.class);
    private static final Pattern JsonErrorPattern = Pattern.compile(".*line: (\\d+), column: (\\d+).*");

    @Autowired
    protected DbService dbService;
    @Autowired
    protected UmaFlow umaFlow;

    @PostConstruct
    public void initialize() {
        Logger.info(String.valueOf(appConfig.getRequestDelaysPoolSize()));
        this.repertoiresDelayer = new Delayer(appConfig.getRequestDelaysPoolSize());
        this.rearrangementsDelayer = new Delayer(appConfig.getRequestDelaysPoolSize());
    }

    /**
     * Handles and logs errors on invalid user JSON body (on POST endpoints) such as invalid syntax or some invalid schema.
     *
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
     *
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
     *
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
     *
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
     * Logs unhandled internal exceptions. Errors here can indicate a logic error or bug.
     *
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
     *
     * @param request      user request
     * @param repertoireId repertoire ID
     * @return the filtered repertoire
     * @throws Exception if user does not have permissions or some other error occurs
     */
    @RequestMapping(
        value = "/repertoire/{repertoireId}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> repertoire(
        @RequestHeader(value = "Content-Protected", defaultValue = "false") Boolean contentProtected,
        HttpServletRequest request,
        @PathVariable String repertoireId
    ) throws Exception {
        RepertoireDto repertoire = new RepertoireDto(repertoireId, adcClient, dbService);

        if (contentProtected) {
            var bearer = SpringUtils.getBearer(request);
            repertoire.processUma(bearer, umaFlow);
        }


        RepertoireResource resource = new RepertoireResource(repertoireId, adcClient, dbService, csvConfig);

        if (contentProtected) {
            var bearer = SpringUtils.getBearer(request);
            resource.enableUma(bearer, umaFlow);
        }

        return resource.response();
    }

    /**
     * Protected by UMA. Individual rearrangement. Part of ADC v1.
     *
     * @param request         user request
     * @param rearrangementId rearrangement ID
     * @return the filtered rearrangement
     * @throws Exception if user does not have permissions or some other error occurs
     */
    @RequestMapping(
        value = "/rearrangement/{rearrangementId}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> rearrangement(
        @RequestHeader(value = "Content-Protected", defaultValue = "false") Boolean contentProtected,
        HttpServletRequest request,
        @PathVariable String rearrangementId
    ) throws Exception {
        RearrangementResource resource = new RearrangementResource(rearrangementId, adcClient, dbService, csvConfig);

        if (contentProtected) {
            var bearer = SpringUtils.getBearer(request);
            resource.enableUma(bearer, umaFlow);
        }

        return resource.response();
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
        method = { RequestMethod.GET, RequestMethod.POST },
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> repertoireList(
        @RequestHeader(value = "Content-Protected", defaultValue = "false") Boolean contentProtected,
        HttpServletRequest request,
        @RequestBody AdcSearchRequest adcSearch
    ) throws Exception {
        this.validateAdcSearch(adcSearch, FieldClass.REPERTOIRE, false);
        RepertoireSet repertoireResource = new RepertoireSet(adcSearch, adcClient, dbService, csvConfig);

        if (contentProtected) {
            var bearer = SpringUtils.getBearer(request);
            repertoireResource.enableUma(bearer, this.umaFlow);
        }

        return repertoireResource.response();
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
        method = { RequestMethod.GET, RequestMethod.POST },
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> rearrangementList(
        @RequestHeader(value = "Content-Protected", defaultValue = "false") Boolean contentProtected,
        HttpServletRequest request,
        @RequestBody AdcSearchRequest adcSearch
    ) throws Exception {
        validateAdcSearch(adcSearch, FieldClass.REARRANGEMENT, true);
        RearrangementSet rearrangementResource = new RearrangementSet(adcSearch, adcClient, dbService, csvConfig);

        if (contentProtected) {
            var bearer = SpringUtils.getBearer(request);
            rearrangementResource.enableUma(bearer, this.umaFlow);
        }

        return rearrangementResource.response();
    }
}
