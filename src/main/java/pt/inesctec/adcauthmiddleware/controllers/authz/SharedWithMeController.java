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
import pt.inesctec.adcauthmiddleware.uma.dto.internal.TokenIntrospection;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.utils.Utils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

@RestController
public class SharedWithMeController extends AuthzController {
    public static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AuthzController.class);

    @RequestMapping(
            value = "/shared_with_me",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArrayList> sharedWithMe(
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        checkRequestValidity(bearer);

        var ownerId = (String) umaClient.getUserInfo(bearer).get("sub");

        String stringUri = umaClient.getIssuer()
                + "/authz/protection/permission/ticket"
                + "?granted=true"
                + "&requester=" + ownerId
                + "&returnNames=true";
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
            Logger.error("Failed to get resources shared with me because: " + e.getMessage());
            throw e;
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(
            value = "/test",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenIntrospection> tester(
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);

        return new ResponseEntity<>(introspection, HttpStatus.OK);
    }
}
