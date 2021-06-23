package pt.inesctec.adcauthmiddleware.controllers.authz;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.controllers.AuthzController;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@RestController
public class RevokeController extends AuthzController {
    public static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AuthzController.class);

    @RequestMapping(
            value = "/revoke",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> revokeAccess(
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        checkRequestValidity(bearer);

        var ownerId = (String) umaClient.getUserInfo(bearer).get("sub");
        var patToken = (String) umaClient.getPat().get("access_token");

        var requester = (String) request.getParameter("requester");
        var resourceId = (String) request.getParameter("resource_id");

        String stringUri = umaClient.getIssuer()
                + "/authz/protection/permission/ticket";

        String stringUriTickets = stringUri
                + "?owner=" + ownerId
                + "&requester=" + requester
                + "&resourceId=" + resourceId
                + "&granted=true";
        var uriTickets = Utils.buildUrl(stringUriTickets);

        var toRequestTickets = new HttpRequestBuilderFacade()
                .getJson(uriTickets)
                .withBearer(patToken)
                .expectJson()
                .build();

        try {
            ArrayList tickets = (ArrayList) HttpFacade.makeExpectJsonRequest(toRequestTickets, ArrayList.class);
            if (tickets != null && tickets.size() > 0) {
                for (var ticket : tickets) {
                    LinkedHashMap ticketMap = (LinkedHashMap) ticket;
                    String delUri = stringUri + "/" + ticketMap.get("id");
                    var uri = Utils.buildUrl(delUri);

                    var toRequest = new HttpRequestBuilderFacade()
                            .delete(uri)
                            .withBearer(patToken)
                            .build();
                    HttpFacade.makeRequest(toRequest);
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to revoke access because: " + e.getMessage());
            throw e;
        }
        return new ResponseEntity<>("", HttpStatus.OK);
    }
}
