package pt.inesctec.adcauthmiddleware.controllers.authz;

import com.google.common.collect.ImmutableMap;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.controllers.AuthzController;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;

import javax.servlet.http.HttpServletRequest;

@RestController
public class GiveAccessController extends AuthzController {
    public static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AuthzController.class);

    @RequestMapping(
            value = "/give_access/{ticketId}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> giveAccess(
            @PathVariable String ticketId,
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        checkRequestValidity(bearer);

        var patToken = (String) umaClient.getPat().get("access_token");

        if (!ticketId.equals("null")) {
            removeTicket(ticketId, patToken);
        }

        var resourceId = (String) request.getParameter("resource_id");
        var requester = (String) request.getParameter("requester_id");
        var scopeName = (String) request.getParameter("scope_name");
        var ownerId = (String) umaClient.getUserInfo(bearer).get("sub");

        String stringUri = umaClient.getKeycloakExtensionApiUri()
//        String stringUri = "http://localhost:5000"
                + "/give_access/adc-middleware"; //TODO - remove hard coded adc-middleware and base url
        var uri = Utils.buildUrl(stringUri);

        var form = ImmutableMap.of(
                "resource_id", resourceId,
                "requester", requester,
                "scope_name", scopeName,
                "owner_id", ownerId
        );

        var toRequest = new HttpRequestBuilderFacade()
                .postForm(uri, form)
                .withBearer(bearer)
                .expectJson()
                .build();

        try {
            HttpFacade.makeRequest(toRequest);
        } catch (Exception e) {
            Logger.error("Failed to give access because: " + e.getMessage());
            throw e;
        }

        return new ResponseEntity<>("", HttpStatus.OK);
    }
}
