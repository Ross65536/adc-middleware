package pt.inesctec.adcauthmiddleware.adc;

public enum UmaScopes {
  SEQUENCE("sequence"),
  REPERTOIRE("repertoire"),
  STATISTICS("statistics");


  private final String umaValue;

  UmaScopes(final String umaValue) {
    this.umaValue = umaValue;
  }

  @Override
  public String toString() {
    return this.umaValue;
  }
}
