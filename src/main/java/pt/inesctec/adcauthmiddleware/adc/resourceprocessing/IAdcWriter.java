package pt.inesctec.adcauthmiddleware.adc.resourceprocessing;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import pt.inesctec.adcauthmiddleware.utils.ThrowingConsumer;

/**
 * Implementing classes are responsible for transcoding the JSON object/resource to their corresponding format.
 */
public interface IAdcWriter {
    /**
     * Close stream.
     *
     * @throws IOException on error
     */
    void close() throws IOException;

    /**
     * Write a JSON object field like 'Info'.
     *
     * @param field the field name
     * @param value the object value
     * @throws IOException on error
     */
    void writeField(String field, ObjectNode value) throws IOException;

    /**
     * Write a JSON array. For the resource list.
     * If {@link #writeField(String, ObjectNode)} is called after this the function returned by this method must be discarded.
     *
     * @param fieldName the array field name
     * @return the consumer that takes individual list elements and writes to output. Each element should be written in streaming mode.
     * @throws IOException on error
     */
    ThrowingConsumer<ObjectNode, IOException> buildArrayWriter(String fieldName) throws IOException;
}
