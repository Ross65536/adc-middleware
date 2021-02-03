package pt.inesctec.adcauthmiddleware.uma;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.controllers.AdcAuthController;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

public class UmaUtils {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcAuthController.class);

    /**
     * Build mapper function from UMA ID to the permitted fields for each resource for the user, given by the UMA resource list.
     * If access is not granted for a resource the public fields for the resource type are returned, if there are any.
     * Used for non-facets regular searches or individual endpoints.
     *
     * @param resources  the UMA resource list with their scopes
     * @param fieldClass the resource type
     * @return the mapper function
     */
    public static Function<String, Set<String>> buildFieldMapper(
        Collection<UmaResource> resources, FieldClass fieldClass, CsvConfig csvConfig) {
        // TODO: Check for fine-grained field accessibility could be added here
        var validUmaFields= resources.stream().collect(
            Collectors.toMap(
                UmaResource::getUmaResourceId,
                uma -> csvConfig.getFields(fieldClass, uma.getScopes())
            )
        );

        var publicFields= csvConfig.getPublicFields(fieldClass);

        return umaId -> {
            if (umaId == null) {
                Logger.warn(
                    "A resource was returned by the repository with no mapping from resource ID to UMA ID. Consider synchronizing.");
                return publicFields;
            }

            // TODO: Check for public study could be placed here?

            // Default Empty Set
            var fields= validUmaFields.getOrDefault(umaId, ImmutableSet.of());
            return Sets.union(fields, publicFields);
        };
    }
}
