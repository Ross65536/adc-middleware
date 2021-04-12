package pt.inesctec.adcauthmiddleware.uma.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for UMA resources. Used to interact with the UMA service.
 * Provides UMA ID and its scopes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UmaResource {
    @JsonProperty("resource_id")
    private String umaId;

    @JsonProperty("resource_scopes")
    private Set<String> scopes;

    public UmaResource() {}

    public UmaResource(String umaId, String... scopes) {
        this(umaId, new HashSet<>(List.of(scopes)));
    }

    public UmaResource(String umaId, Set<String> umaScopes) {
        this.umaId = umaId;
        this.scopes = umaScopes;
    }

    public String getUmaId() {
        return umaId;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}
