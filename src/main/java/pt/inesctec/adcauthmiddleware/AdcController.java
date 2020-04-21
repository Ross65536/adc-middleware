package pt.inesctec.adcauthmiddleware;

import java.net.URI;
import java.net.http.HttpRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.adc.UmaScopes;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;
import pt.inesctec.adcauthmiddleware.config.UmaConfig;
import pt.inesctec.adcauthmiddleware.http.HttpFacade;
import pt.inesctec.adcauthmiddleware.http.HttpRequestBuilderFacade;
import pt.inesctec.adcauthmiddleware.uma.UmaClient;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;


@RestController
public class AdcController {

  private AdcConfiguration adcConfig;
  private UmaClient umaClient;

  @Autowired
  public AdcController(AdcConfiguration adcConfig, UmaConfig umaConfig) throws Exception {
    this.adcConfig = adcConfig;

    this.umaClient = new UmaClient(umaConfig);
  }

  @RequestMapping(
      value = "/study/{studyId}/repertoire/{repertoireId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity repertoire(@PathVariable String studyId, @PathVariable String repertoireId)
      throws Exception {
    final URI uri = this.getResourceServerPath("study", studyId, "repertoire", repertoireId);
    final String UmaResourceId = "87e43a0e-9108-41ac-a9da-bee2e3b9bb12";

    var umaResource = new UmaResource(UmaResourceId, UmaScopes.SEQUENCE.toString()); // repertoire is level 3
    var ticket = this.umaClient.requestPermissionsTicket(umaResource);


    HttpRequest request = new HttpRequestBuilderFacade()
        .getJson(uri)
        .build();
    var response = HttpFacade.makeExpectJsonStringRequest(request);

    return new ResponseEntity(ticket + response, HttpStatus.OK);
  }

  private URI getResourceServerPath(String... parts) {
    final String basePath = adcConfig.getResourceServerUrl();
    return Utils.buildUrl(basePath, parts);
  }
}
