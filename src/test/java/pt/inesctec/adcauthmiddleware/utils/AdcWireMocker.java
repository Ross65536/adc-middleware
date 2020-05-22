package pt.inesctec.adcauthmiddleware.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.Map;

public final class AdcWireMocker {



  public static void wireRepertoiresSearch(WireMockServer backendMock, Object responseDocument, Object expectedAdc) throws JsonProcessingException {
    WireMocker.wirePostJson(backendMock, TestConstants.buildAirrPath("repertoire"), 200, responseDocument, expectedAdc);
  }
}
