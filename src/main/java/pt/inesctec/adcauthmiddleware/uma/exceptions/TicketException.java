package pt.inesctec.adcauthmiddleware.uma.exceptions;

/**
 * Represent a UMA permission ticket. Supposed to be thrown and caught by spring.
 */
public class TicketException extends Exception {

  private String ticket;
  private String issuer;

  public TicketException(String ticket, String issuer) {
    super("Can't access resource because no RPT token present");
    this.ticket = ticket;
    this.issuer = issuer;
  }

  /**
   * Builds the 'WWW-Authenticate' header according to the UMA spec.
   *
   * @return the header's value.
   */
  public String buildAuthenticateHeader() {
    return "UMA as_uri=\"" + issuer + "\", ticket=\"" + ticket + "\"";
  }
}
