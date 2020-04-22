package pt.inesctec.adcauthmiddleware.uma;

import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

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

  private void exactMatch(String bearerToken, UmaResource[] resources) {

  }
}
