package pt.inesctec.adcauthmiddleware.controllers;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.db.services.SynchronizeService;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;

@RestController @RequestMapping("${app.airrBasepath}")
public class SynchronizeController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(SynchronizeController.class);

    @Autowired
    protected SynchronizeService synchronizeService;

    @Autowired
    protected AppConfig appConfig;

    @Autowired
    protected UmaClient umaClient;

    /**
     * Logs synchronization endpoint user errors.
     *
     * @param e exception
     * @return 401 status code
     */
    @ExceptionHandler(SyncException.class)
    public ResponseEntity<String> synchronizeErrorHandler(SyncException e) {
        Logger.info("Synchronize: {}", e.getMessage());
        return SpringUtils.buildJsonErrorResponse(HttpStatus.UNAUTHORIZED, null);
    }

    /**
     * The synchronize endpoint. Not part of ADC v1. Protected by password set in the configuration file. Extension of the middleware.
     * Performs state synchronization between the repository and the UMA authorization server and this middleware's DB.
     * Resets the delays pool request times.
     *
     * @param request the user request
     * @return OK on successful synchronization or an error code when a process in the synchronization fails.
     * @throws Exception on user errors such as invalid password or some internal errors.
     */
    @RequestMapping(value = "/synchronize", method = RequestMethod.POST)
    public Map<String, Object> synchronize(HttpServletRequest request) throws Exception {
        String bearer = SpringUtils.getBearer(request);

        if (bearer == null) {
            throw new SyncException("Invalid user credential format");
        }

        var tokenResources = this.umaClient.introspectToken(bearer, false);

        if (!tokenResources.getRoles().contains(this.appConfig.getSynchronizeRole())) {
            throw new SyncException("User not allowed to synchronize resources");
        }

        if (!this.synchronizeService.synchronize()) {
            throw SpringUtils.buildHttpException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "One or more DB or UMA resources failed to synchronize, check logs");
        }

        return SpringUtils.buildStatusMessage(200, null);
    }
}
