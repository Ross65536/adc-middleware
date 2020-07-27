package pt.inesctec.adcauthmiddleware.adc.models;

/**
 * Models an ADC request semantic error.
 */
public class AdcException extends Exception {

  public AdcException(String msg) {
    super(msg);
  }
}
