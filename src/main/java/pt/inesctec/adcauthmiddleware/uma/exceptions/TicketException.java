package pt.inesctec.adcauthmiddleware.uma.exceptions;

public class TicketException extends Exception {

  private String ticket;
  private String issuer;

  public TicketException(String ticket, String issuer) {
    super("Can't access resource because no RPT token present");
    this.ticket = ticket;
    this.issuer = issuer;
  }

  public String getTicket() {
    return ticket;
  }

  public String buildAuthenticateHeader() {
    return "UMA as_uri=\"" + issuer + "\", ticket=\"" + ticket + "\"";
  }
}
