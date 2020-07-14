package pt.inesctec.adcauthmiddleware.adc.jsonfilter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import pt.inesctec.adcauthmiddleware.utils.ThrowingConsumer;

public interface IAdcWriter {
  void close() throws IOException;

  void writeField(String field, ObjectNode value) throws IOException;

  ThrowingConsumer<ObjectNode, IOException> buildArrayWriter(String fieldName)
      throws IOException;
}
