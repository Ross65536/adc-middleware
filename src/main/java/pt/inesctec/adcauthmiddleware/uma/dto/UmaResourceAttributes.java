package pt.inesctec.adcauthmiddleware.uma.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UmaResourceAttributes {
    private Set<String> publicFields;
    private Set<String> rawSequenceFields;
    private Set<String> statisticFields;

    @JsonProperty("public")
    public Set<String> getPublicFields() {
        return publicFields;
    }

    @JsonProperty("public")
    public void setPublicFields(Set<String> publicFields) {
        this.publicFields = publicFields;
    }

    @JsonProperty("raw_sequence")
    public Set<String> getRawSequenceFields() {
        return rawSequenceFields;
    }

    @JsonProperty("raw_sequence")
    public void setRawSequenceFields(Set<String> rawSequenceFields) {
        this.rawSequenceFields = rawSequenceFields;
    }

    @JsonProperty("statistics")
    public Set<String> getStatisticFields() {
        return statisticFields;
    }

    @JsonProperty("statistics")
    public void setStatisticFields(Set<String> statisticFields) {
        this.statisticFields = statisticFields;
    }
}
