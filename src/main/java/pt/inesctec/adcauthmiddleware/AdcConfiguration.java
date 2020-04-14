package pt.inesctec.adcauthmiddleware;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Primary
@Validated
@Configuration
@ConfigurationProperties(prefix= "adc")
public class AdcConfiguration {

    @NotNull
    private String resourceServerUrl;

    public String getResourceServerUrl() {
        return resourceServerUrl;
    }

    public void setResourceServerUrl(String resourceServerUrl) {
        this.resourceServerUrl = resourceServerUrl;
    }
}
