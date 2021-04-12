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
     * Complete URL of AuthZ well-known endpoint.
     * The well-known endpoint contains crucial metadata related to the specified authorization service.
     */
    @NonNull
    @URL(regexp = "^(http|https).*")
    private String wellKnownUrl;

    /**
     * The client ID set for the middleware in AuthZ Service.
     */
    @NonNull
    @NotBlank
    private String clientId;

    /**
     * The client secret set for the middleware in the AuthZ Service.
     */
    @NonNull
    @NotBlank
    private String clientSecret;

    /**
     * Username of the UMA user which will be set as the resource owner for all of the created resources.
     * Keycloak specific.
     */
    @NonNull
    @NotBlank
    private String resourceOwner;

    /**
     * Name of the Access Scope name shared between the UMA.
     */
    private String publicScopeName;

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

    // Will return null when not set in the .properties file or when it's defined as an empty string
    public String getPublicScopeName() {
        if (publicScopeName.isEmpty()) {
            return null;
        }
        return publicScopeName;
    }

    public void setPublicScopeName(String publicScopeName) {
        this.publicScopeName = publicScopeName;
    }
}
