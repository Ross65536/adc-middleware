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

@RestController
public class OwnResourcesController extends AuthzController {
    public static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AuthzController.class);

    @RequestMapping(
            value = "/own_resources",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArrayList> ownResources(
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        checkRequestValidity(bearer);

        var username = (String) umaClient.getUserInfo(bearer).get("preferred_username");

        String stringUri = umaClient.getIssuer()
                + "/authz/protection/resource_set"
                + "?owner=" + username
                + "&deep=true";
        var uri = Utils.buildUrl(stringUri);

        var toRequest = new HttpRequestBuilderFacade()
                .getJson(uri)
                .withBearer(umaClient.getAccessToken().getAccessToken())
                .expectJson()
                .build();

        ArrayList response = null;
        try {
            response = (ArrayList) HttpFacade.makeExpectJsonRequest(toRequest, ArrayList.class);
        } catch (Exception e) {
            Logger.error("Failed to get own resources because: " + e.getMessage());
            throw e;
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
