package pt.inesctec.adcauthmiddleware.uma;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.inesctec.adcauthmiddleware.Utils;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;
import pt.inesctec.adcauthmiddleware.uma.models.internal.AccessToken;
import pt.inesctec.adcauthmiddleware.uma.models.internal.Ticket;
import pt.inesctec.adcauthmiddleware.uma.models.internal.UmaWellKnown;

import java.util.Map;

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
    var request = HttpFacade.buildJsonPostExpectJsonRequest(uri, resources);
    request = HttpFacade.addRequestBearer(request, this.accessToken.getAccessToken());

    var ticket = HttpFacade.makeExpectJsonRequest(request.build(), Ticket.class);
    return ticket.getTicket();
  }

  private void updateAccessToken() throws Exception {

    Logger.info("Getting new UMA access token");
    var body = Map.of(
        "grant_type", "client_credentials",
        "client_id", this.umaConfig.getClientId(),
        "client_secret", this.umaConfig.getClientSecret()
    );

    var uri = Utils.buildUrl(wellKnown.getTokenEndpoint());
    AccessToken accessToken = null;
    var request = HttpFacade.buildPostFormExpectJsonRequest(uri, body);
    try {
      accessToken = HttpFacade.makeExpectJsonRequest(request, AccessToken.class);
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
    var request = HttpFacade.buildGetJsonRequest(uri);
    try {
      var obj = HttpFacade.makeExpectJsonRequest(request, UmaWellKnown.class);
      Utils.jaxValidate(obj);
      return obj;
    } catch (Exception e) {
      Logger.error("Failed to fetch UMA 2 well known document at: {} because: {}", wellKnownUrl, e.getMessage());
      throw e;
    }
  }

}
