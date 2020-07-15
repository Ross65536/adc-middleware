package pt.inesctec.adcauthmiddleware.adc.resourceprocessing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;

public interface IFieldsFilter {
  Optional<ObjectNode> mapResource(ObjectNode node);

  IFieldsFilter BlockingFilter = (node) -> Optional.empty();
  IFieldsFilter OpenFilter = Optional::of;
}
