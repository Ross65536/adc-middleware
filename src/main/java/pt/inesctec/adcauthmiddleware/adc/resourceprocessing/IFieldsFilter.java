package pt.inesctec.adcauthmiddleware.adc.resourceprocessing;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents a filter operator. Responsible for filtering out fields from an ADC resource like repertoire or rearrangement.
 */
public interface IFieldsFilter {
    /**
     * Removes all fields from resource.
     */
    IFieldsFilter BlockingFilter = (node) -> Optional.empty();
    /**
     * Removes NONE fields from resource.
     */
    IFieldsFilter OpenFilter = Optional::of;

    Optional<ObjectNode> mapResource(ObjectNode node);
}
