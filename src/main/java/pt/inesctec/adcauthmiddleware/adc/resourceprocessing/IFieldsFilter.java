package pt.inesctec.adcauthmiddleware.adc.resourceprocessing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;

/**
 * Represents a filter operator. Responsible for filtering out fields from an ADC resource like repertoire or rearrangement.
 */
public interface IFieldsFilter {
  Optional<ObjectNode> mapResource(ObjectNode node);

  /**
   * Removes all fields from resource.
   */
  IFieldsFilter BlockingFilter = (node) -> Optional.empty();
  /**
   * Removes NONE fields from resource.
   */
  IFieldsFilter OpenFilter = Optional::of;
}
