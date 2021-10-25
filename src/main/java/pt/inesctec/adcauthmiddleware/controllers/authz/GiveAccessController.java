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
import java.util.ArrayList;
import java.util.LinkedHashMap;

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

        if (!ticketId.equals("null")) {
            removeTicket(ticketId, umaClient.getAccessToken().getAccessToken());
        }

        var resourceId = (String) request.getParameter("resource_id");
        var requester = (String) request.getParameter("requester_id");
        var scopeName = (String) request.getParameter("scope_name");
        var ownerId = (String) umaClient.getUserInfo(bearer).get("sub");

        String stringUri = umaClient.getKeycloakExtensionApiUri()
                + "/get_user_scope_id/" + requester;

        var uri = Utils.buildUrl(stringUri);

        var form = ImmutableMap.of(
                "owner_id", ownerId,
                "scope_name", scopeName
        );

        var toRequest = new HttpRequestBuilderFacade()
                .postForm(uri, form)
                .withBearer(bearer)
                .expectJson()
                .build();

        try {
            ArrayList<String> response = (ArrayList) HttpFacade.makeExpectJsonRequest(toRequest, ArrayList.class);
            stringUri = umaClient.getIssuer()
                    + "/authz/protection/permission/ticket";

            uri = Utils.buildUrl(stringUri);

            form = ImmutableMap.of(
                    "resource", resourceId,
                    "requester", response.get(0),
                    "scopeName", scopeName,
                    "granted", "true"
            );

            toRequest = new HttpRequestBuilderFacade()
                    .postJson(uri, form)
                    .withBearer(umaClient.exchangeToken(bearer).getAccessToken())
                    .expectJson()
                    .build();

            try {
                HttpFacade.makeRequest(toRequest);
            } catch (Exception e) {
                String stringUriTemp = umaClient.getIssuer()
                        + "/authz/protection/permission/ticket"
                        + "?granted=false"
                        + "&owner=" + ownerId
                        + "&resourceId=" + resourceId
                        + "&requester=" + response.get(0)
                        + "&scopeId=" + response.get(1);

                var uriTemp = Utils.buildUrl(stringUriTemp);

                var toRequestTemp = new HttpRequestBuilderFacade()
                        .getJson(uriTemp)
                        .withBearer(umaClient.getAccessToken().getAccessToken())
                        .expectJson()
                        .build();

                ArrayList responseTemp = (ArrayList) HttpFacade.makeExpectJsonRequest(toRequestTemp, ArrayList.class);

                if (responseTemp.size() != 1) {
                    throw new Exception("Ticket finding error");
                }

                stringUriTemp = umaClient.getIssuer()
                        + "/authz/protection/permission/ticket/" + ((LinkedHashMap) responseTemp.get(0)).get("id");
                uriTemp = Utils.buildUrl(stringUriTemp);

                toRequestTemp = new HttpRequestBuilderFacade()
                        .delete(uriTemp)
                        .withBearer(umaClient.getAccessToken().getAccessToken())
                        .build();
                HttpFacade.makeRequest(toRequestTemp);

                HttpFacade.makeRequest(toRequest);
            }
        } catch (Exception e) {
            Logger.error("Failed to give access because: " + e.getMessage());
            throw e;
        }

        return new ResponseEntity<>("", HttpStatus.OK);
    }
}
