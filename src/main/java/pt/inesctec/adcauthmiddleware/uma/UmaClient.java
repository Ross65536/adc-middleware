package pt.inesctec.adcauthmiddleware.uma;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.Utils;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;
import pt.inesctec.adcauthmiddleware.uma.exceptions.UmaFlowException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaRegistrationResource;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;
import pt.inesctec.adcauthmiddleware.uma.models.internal.*;

import java.util.List;
import java.util.Map;

import static pt.inesctec.adcauthmiddleware.http.HttpFacade.makeExpectJsonRequest;

@Component
public class UmaClient {
  private static Logger Logger = LoggerFactory.getLogger(UmaClient.class);

  private UmaConfig umaConfig;
  private UmaWellKnown wellKnown;
  private AccessToken accessToken = null;

  public UmaClient(UmaConfig config) throws Exception {
    this.umaConfig = config;
    this.wellKnown = UmaClient.getWellKnown(config.getWellKnownUrl());

    this.updateAccessToken();
  }

  public String requestPermissionsTicket(UmaResource ... resources) throws Exception {
    this.updateAccessToken();
    var uri = Utils.buildUrl(wellKnown.getPermissionEndpoint());
    var request = new HttpRequestBuilderFacade()
        .postJson(uri, resources)
        .expectJson()
        .withBearer(this.accessToken.getAccessToken())
        .build();

    try {
      return makeExpectJsonRequest(request, Ticket.class)
          .getTicket();
    } catch (Exception e) {
      Logger.info("Failed to get permissions ticket because: " + e.getMessage());
      throw e;
    }
  }



  public String getIssuer() {
    return this.wellKnown.getIssuer();
  }

  public List<UmaResource> introspectToken(String rptToken) throws Exception {
    this.updateAccessToken();
    var uri = Utils.buildUrl(wellKnown.getIntrospectionEndpoint());
    var form = ImmutableMap.of("token", rptToken, "token_type_hint", "requesting_party_token");
    var request = new HttpRequestBuilderFacade()
        .postForm(uri, form)
        .expectJson()
        // TODO update to bearer once keycloak follows spec
        // Keycloak doesn't follow UMA spec in allowing UMA access tokens to be used here (Bearer).
        .withBasicAuth(this.umaConfig.getClientId(), this.umaConfig.getClientSecret())
        .build();

    TokenIntrospection introspection = null;
    try {
      introspection = HttpFacade.makeExpectJsonRequest(request, TokenIntrospection.class);
    } catch (Exception e) {
      Logger.info("Failed to get permissions ticket because: " + e.getMessage());
      throw e;
    }

    if (! introspection.isActive()) {
      throw new UmaFlowException("RPT token is invalid (not active)");
    }

    return introspection.getPermissions();
  }

  private void updateAccessToken() throws Exception {
    // TODO only update token if necessary, use refresh token

    Logger.info("Getting new UMA access token");
    var body = Map.of(
        "grant_type", "client_credentials",
        "client_id", this.umaConfig.getClientId(),
        "client_secret", this.umaConfig.getClientSecret()
    );

    var uri = Utils.buildUrl(wellKnown.getTokenEndpoint());
    AccessToken accessToken = null;
    var request = new HttpRequestBuilderFacade()
        .postForm(uri, body)
        .expectJson()
        .build();

    try {
      accessToken = makeExpectJsonRequest(request, AccessToken.class);
      Utils.jaxValidate(accessToken);
    } catch (Exception e) {
      Logger.error("Failed to get UMA access token because: {}", e.getMessage());
      throw e;
    }

    this.accessToken = accessToken;
  }

  private static UmaWellKnown getWellKnown(String wellKnownUrl) throws Exception {
    Logger.info("Requesting UMA 2 well known doc at: {}", wellKnownUrl);
    var uri = Utils.buildUrl(wellKnownUrl);
    var request = new HttpRequestBuilderFacade()
        .getJson(uri)
        .build();
    try {
      var obj = makeExpectJsonRequest(request, UmaWellKnown.class);
      Utils.jaxValidate(obj);
      return obj;
    } catch (Exception e) {
      Logger.error("Failed to fetch UMA 2 well known document at: {} because: {}", wellKnownUrl, e.getMessage());
      throw e;
    }
  }

  public String[] listUmaResources() throws Exception {
    this.updateAccessToken();

    Logger.info("Requesting UMA 2 resource list");

    var uri = Utils.buildUrl(wellKnown.getResourceRegistrationEndpoint());
    var request = new HttpRequestBuilderFacade()
        .getJson(uri)
        .withBearer(this.accessToken.getAccessToken())
        .build();

    try {
      return makeExpectJsonRequest(request, String[].class);
    } catch (Exception e) {
      Logger.error("Failed to get UMA resource list: {}", e.getMessage());
      throw e;
    }
  }

  public void deleteUmaResource(String umaId) throws Exception {
    this.updateAccessToken();

    Logger.info("Deleting UMA 2 resource: {}", umaId);

    var uri = Utils.buildUrl(wellKnown.getResourceRegistrationEndpoint(), umaId);
    var request = new HttpRequestBuilderFacade()
        .delete(uri)
        .withBearer(this.accessToken.getAccessToken())
        .build();

    try {
      HttpFacade.makeRequest(request);
    } catch (Exception e) {
      Logger.error("Failed to delete UMA resource {} because: {}", umaId, e.getMessage());
      throw e;
    }
  }

  public String createUmaResource(UmaRegistrationResource resource) throws Exception {
    this.updateAccessToken();

    resource.setId(null);
    resource.setOwnerManagedAccess(true);
    resource.setOwner(this.umaConfig.getResourceOwner());
    Logger.info("Creating UMA 2 resource: {}", resource);

    var uri = Utils.buildUrl(wellKnown.getResourceRegistrationEndpoint());
    var request = new HttpRequestBuilderFacade()
        .postJson(uri, resource)
        .expectJson()
        .withBearer(this.accessToken.getAccessToken())
        .build();

    String createdId = null;
    try {
      createdId = HttpFacade.makeExpectJsonRequest(request, UmaResourceCreate.class)
          .getId();
    } catch (Exception e) {
      Logger.error("Failed to create UMA resource {} because: {}", resource, e.getMessage());
      throw e;
    }

    Utils.assertNotNull(createdId);

    return createdId;
  }
}
