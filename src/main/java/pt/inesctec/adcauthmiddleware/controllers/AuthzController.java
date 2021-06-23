package pt.inesctec.adcauthmiddleware.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.dto.internal.TokenIntrospection;
import pt.inesctec.adcauthmiddleware.utils.Utils;

@CrossOrigin(origins = "${app.resourceAllowedOrigins}")
@RequestMapping("/authz")
public abstract class AuthzController {
    private static final Logger Logger = LoggerFactory.getLogger(AuthzController.class);

    @Autowired
    protected AppConfig appConfig;
    @Autowired
    protected UmaClient umaClient;

    protected void removeTicket(String ticketId, String patToken) throws Exception{
        String stringUri = umaClient.getIssuer()
                + "/authz/protection/permission/ticket/"
                + ticketId;
        var uri = Utils.buildUrl(stringUri);

        var request = new HttpRequestBuilderFacade()
                .delete(uri)
                .withBearer(patToken)
                .build();

        HttpFacade.makeRequest(request);
    }

    protected void checkRequestValidity(String bearer) throws Exception {
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);
        if (!introspection.isActive()) {
            throw new Exception("Access token is not active");
        }
    }
}
