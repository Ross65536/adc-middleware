package pt.inesctec.adcauthmiddleware.uma.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

/**
 * Models a UMA PAT token, used by the client internaly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessToken {

  @JsonProperty("access_token")
  @NotNull
  private String accessToken;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }
}
