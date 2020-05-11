package pt.inesctec.adcauthmiddleware.config;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

@Primary
@Validated
@Configuration
@ConfigurationProperties(prefix = "adc")
public class AdcConfiguration {

  @NotNull
  @URL(regexp = "^(http|https).*")
  private String resourceServerUrl;

  public String getResourceServerUrl() {
    return resourceServerUrl;
  }

  public void setResourceServerUrl(String resourceServerUrl) {
    this.resourceServerUrl = resourceServerUrl;
  }
}
