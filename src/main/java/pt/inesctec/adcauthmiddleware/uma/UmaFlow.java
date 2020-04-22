package pt.inesctec.adcauthmiddleware.uma;

import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.exceptions.UmaFlowException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class UmaFlow {
  private final UmaClient umaClient;

  public UmaFlow(UmaClient umaClient) {
    this.umaClient = umaClient;
  }

  public void exactMatchFlow(String bearerToken, UmaResource ... resources) throws Exception {
    if (bearerToken == null) {
      this.noRptToken(resources);
    } else {
      this.exactMatch(bearerToken, resources);
    }
  }

  private void noRptToken(UmaResource[] resources) throws Exception {
    var ticket = this.umaClient.requestPermissionsTicket(resources);
    throw new TicketException(ticket, this.umaClient.getIssuer());
  }

  private void exactMatch(String bearerToken, UmaResource[] actualResources) throws Exception {
    var expectedResources = this.umaClient.introspectToken(bearerToken);

    Map<String, HashSet<String>> expectedMap = expectedResources.stream()
        .collect(Collectors.toMap(UmaResource::getUmaResourceId, e -> new HashSet<>(e.getScopes())));

    var anyDeniedAccess = Arrays.stream(actualResources)
        .anyMatch(actual -> {
          var set = expectedMap.get(actual.getUmaResourceId());
          if (set == null) {
            return false;
          }

          return actual.getScopes()
              .stream()
              .anyMatch(actualScope -> !set.contains(actualScope));
        });

    if (anyDeniedAccess) {
      throw new UmaFlowException("Requested UMA resources not granted access to by RPT token");
    }
  }
}
