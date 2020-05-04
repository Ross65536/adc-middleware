package pt.inesctec.adcauthmiddleware;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpError {
  private int status;
  private String msg;

  public HttpError(int status, String msg) {
    this.status = status;
    this.msg = msg;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

}
