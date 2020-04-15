package pt.inesctec.adcauthmiddleware.config;

import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Validated
@Configuration
@ConfigurationProperties(prefix="uma")
public class UmaConfig {
    @NonNull
    @URL(regexp = "^(http|https).*")
    private String wellKnownUrl;

    @NonNull
    @NotBlank
    private String clientId;

    @NonNull
    @NotBlank
    private String clientSecret;

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
}
