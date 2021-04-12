package pt.inesctec.adcauthmiddleware.uma.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Models the response on creating a UMA resource. Contains the UMA ID.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UmaResourceCreate {
    @JsonProperty("_id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
