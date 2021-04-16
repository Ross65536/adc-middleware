package pt.inesctec.adcauthmiddleware.adc.resourceprocessing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.adc.AdcConstants;
import pt.inesctec.adcauthmiddleware.http.Json;

/**
 * JSON parser for repository response. Both regular and facets.
 * Response must follow ADC JSON schema.
 */
public class AdcJsonDocumentParser {
    /**
     * Shared JSON factory singleton.
     */
    public static JsonFactory JsonFactory = new JsonFactory();
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcJsonDocumentParser.class);

    static {
        JsonFactory.setCodec(Json.JsonObjectMapper);
    }

    private final InputStream inputStream;
    private final String mappedField;
    private final IFieldsFilter filter;
    private IAdcWriter adcWriter;

    /**
     * constructor.
     *
     * @param inputStream the input ADC JSON byte stream coming from the ADC Service
     * @param mappedField the ADC response document's field name of the resources.
     * @param filter      the filter function. Will modify the resource by removing/adding fields
     * @param adcWriter   the writer interface and ultimately the output
     */
    private AdcJsonDocumentParser(InputStream inputStream, String mappedField, IFieldsFilter filter, IAdcWriter adcWriter) {
        this.filter = filter;
        this.inputStream = inputStream;
        this.mappedField = mappedField;
        this.adcWriter = adcWriter;
    }

    /**
     * Build a JSON to JSON processor.
     *
     * @param inputStream    the JSON input
     * @param mappedField the mapped ADC field name
     * @param filter      the resource filter
     * @return spring streaming JSON response
     */
    public static StreamingResponseBody buildJsonMapper(InputStream inputStream, String mappedField, IFieldsFilter filter) {
        return os -> {
            var jsonWriter = new AdcJsonWriter(os);
            var mapper = new AdcJsonDocumentParser(inputStream, mappedField, filter, jsonWriter);
            mapper.process();
        };
    }

    /**
     * Build a JSON to TSV processor.
     *
     * @param response     the JSON input
     * @param mappedField  the mapped ADC field name
     * @param headers      set of headers that will be included in the TSV
     * @param filter       the resource filter
     * @return spring streaming TSV response
     */
    public static StreamingResponseBody buildTsvMapper(InputStream response, String mappedField, List<String> headers, IFieldsFilter filter) {
        return os -> {
            var tsvWriter = new AdcTsvWriter(os, headers);
            var mapper = new AdcJsonDocumentParser(response, mappedField, filter, tsvWriter);
            mapper.process();
        };
    }

    /**
     * Start processing the inputStream from the ADC Service, apply designated filters and
     * output result to the IAdcWriter interface.
     *
     * @throws IOException on error.
     */
    private void process() throws IOException {
        var parser = JsonFactory.createParser(inputStream);

        if (parser.nextToken() != JsonToken.START_OBJECT) {
            Logger.error("Response from ADC Service isn't a valid JSON object");
            this.adcWriter.close();
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var fieldName = parser.getCurrentName();
            var nextToken = parser.nextToken();
            if (fieldName == null
                    || (nextToken != JsonToken.START_OBJECT && nextToken != JsonToken.START_ARRAY)) {
                Logger.error("Malformed JSON received from ADC Service");
                this.adcWriter.close();
                return;
            }

            if (fieldName.equals(AdcConstants.ADC_INFO)) {
                var map = parser.readValueAs(ObjectNode.class);
                this.adcWriter.writeField(AdcConstants.ADC_INFO, map);
            } else if (fieldName.equals(this.mappedField)) {
                processResources(parser);
            } else {
                parser.skipChildren();
            }
        }

        this.adcWriter.close();
    }

    /**
     * Process the resource list. Each resource's fields are filtered and then written to a specific format.
     *
     * @param parser the input JSON parser.
     * @throws IOException on error.
     */
    private void processResources(JsonParser parser) throws IOException {
        var consumer = this.adcWriter.buildArrayWriter(this.mappedField);

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            var map = parser.readValueAs(ObjectNode.class);
            var mapped = this.filter.mapResource(map);

            if (mapped.isPresent()) {
                consumer.accept(mapped.get());
            }
        }
    }
}
