package pt.inesctec.adcauthmiddleware.config;

import javax.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

/**
 * Model for the configuration file, UMA/Keycloak specific.
 */
@Validated
@Configuration
@ConfigurationProperties(prefix = "uma")
public class UmaConfig {

  /**
   * complete URL for the UMA well known document (discovery document).
   */
  @NonNull
  @URL(regexp = "^(http|https).*")
  private String wellKnownUrl;

  /**
   * The client ID set for the middleware in Keycloak.
   */
  @NonNull @NotBlank private String clientId;

  /**
   * The client secret set for the middleware in Keycloak.
   */
  @NonNull @NotBlank private String clientSecret;

  /**
   * Keycloak specific. The username of the Keycloak user which will be set as the resource owner for all of the created resources.
   */
  @NonNull @NotBlank private String resourceOwner;

  @NonNull
  public String getWellKnownUrl() {
    return wellKnownUrl;
  }

  public void setWellKnownUrl(@NonNull String wellKnownUrl) {
    this.wellKnownUrl = wellKnownUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  @NonNull
  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(@NonNull String clientSecret) {
    this.clientSecret = clientSecret;
  }

  @NonNull
  public String getResourceOwner() {
    return resourceOwner;
  }

  public void setResourceOwner(@NonNull String resourceOwner) {
    this.resourceOwner = resourceOwner;
  }
}
