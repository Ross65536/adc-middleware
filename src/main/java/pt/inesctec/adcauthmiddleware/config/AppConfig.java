package pt.inesctec.adcauthmiddleware.config;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

  private String adcCsvConfigPath;
  private boolean facetsEnabled = true;
  private boolean publicEndpointsEnabled = true;
  @NotNull @NotBlank
  private String synchronizePasswordHash;

  public String getAdcCsvConfigPath() {
    return adcCsvConfigPath;
  }

  public void setAdcCsvConfigPath(String adcCsvConfigPath) {
    this.adcCsvConfigPath = adcCsvConfigPath;
  }

  public String getSynchronizePasswordHash() {
    return synchronizePasswordHash;
  }

  public void setSynchronizePasswordHash(String synchronizePasswordHash) {
    this.synchronizePasswordHash = synchronizePasswordHash;
  }

  public boolean isFacetsEnabled() {
    return facetsEnabled;
  }

  public void setFacetsEnabled(boolean facetsEnabled) {
    this.facetsEnabled = facetsEnabled;
  }

  public boolean isPublicEndpointsEnabled() {
    return publicEndpointsEnabled;
  }

  public void setPublicEndpointsEnabled(boolean publicEndpointsEnabled) {
    this.publicEndpointsEnabled = publicEndpointsEnabled;
  }
}
