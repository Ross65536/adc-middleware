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
public class ResourceDetailsController extends AuthzController {
    public static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AuthzController.class);

    @RequestMapping(
            value = "/resource_details",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArrayList> resourceDetails(
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        checkRequestValidity(bearer);

        var ownerId = (String) umaClient.getUserInfo(bearer).get("sub");
        var patToken = (String) umaClient.getPat().get("access_token");

        var resourceId = (String) request.getParameter("resource_id");

        String stringUri = umaClient.getIssuer()
                + "/authz/protection/permission/ticket"
                + "?resourceId=" + resourceId
                + "&granted=true"
                + "&owner=" + ownerId
                + "&returnNames=true";
        var uri = Utils.buildUrl(stringUri);

        var toRequest = new HttpRequestBuilderFacade()
                .getJson(uri)
                .withBearer(patToken)
                .expectJson()
                .build();

        ArrayList response = null;
        try {
            response = (ArrayList) HttpFacade.makeExpectJsonRequest(toRequest, ArrayList.class);
        } catch (Exception e) {
            Logger.error("Failed to get resource details because: " + e.getMessage());
            throw e;
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
