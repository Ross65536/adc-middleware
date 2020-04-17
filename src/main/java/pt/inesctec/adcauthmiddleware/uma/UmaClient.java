package pt.inesctec.adcauthmiddleware.uma;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.inesctec.adcauthmiddleware.Utils;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.uma.models.UmaWellKnown;

import javax.validation.Validation;

public class UmaClient {
  private static Logger Logger = LoggerFactory.getLogger(UmaClient.class);

  private UmaConfig umaConfig;
  private UmaWellKnown wellKnown;

  public UmaClient(UmaConfig config) throws Exception {
    this.umaConfig = config;
    this.wellKnown = UmaClient.getWellKnown(config.getWellKnownUrl());
  }

  private static UmaWellKnown getWellKnown(String wellKnownUrl) throws Exception {
    Logger.info("Requesting UMA 2 well known doc at: {}", wellKnownUrl);
    var uri = Utils.buildUrl(wellKnownUrl);
    try {
      var obj = HttpFacade.getJson(uri, UmaWellKnown.class);
      Utils.jaxValidate(obj);
      return obj;
    } catch (Exception e) {
      Logger.error("Failed to fetch UMA 2 well known document at: {} because: {}", wellKnownUrl, e.getMessage());
      throw e;
    }
  }

}
