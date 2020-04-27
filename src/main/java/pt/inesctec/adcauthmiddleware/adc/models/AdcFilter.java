package pt.inesctec.adcauthmiddleware.adc.models;

public class AdcFilter {
  private String op;
  private Object content;

  public String getOp() {
    return this.op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public Object getContent() {
    return content;
  }

  public void setContent(Object content) {
    this.content = content;
  }
}
