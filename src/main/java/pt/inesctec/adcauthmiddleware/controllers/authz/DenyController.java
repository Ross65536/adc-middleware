package pt.inesctec.adcauthmiddleware.controllers.authz;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.controllers.AuthzController;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;

import javax.servlet.http.HttpServletRequest;

@RestController
public class DenyController extends AuthzController {
    public static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AuthzController.class);

    @RequestMapping(
            value = "/deny/{ticketId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> deny(
            @PathVariable String ticketId,
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        checkRequestValidity(bearer);

        try {
            removeTicket(ticketId, umaClient.getAccessToken().getAccessToken());
        } catch (Exception e) {
            Logger.error("Failed to get denying request because: " + e.getMessage());
            throw e;
        }
        return new ResponseEntity<>("", HttpStatus.OK);
    }
}
