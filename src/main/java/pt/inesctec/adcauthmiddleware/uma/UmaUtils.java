package pt.inesctec.adcauthmiddleware.uma;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import pt.inesctec.adcauthmiddleware.adc.resources.AdcResource;
import pt.inesctec.adcauthmiddleware.config.csv.CsvConfig;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.controllers.AdcAuthController;
import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;

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
        Collection<UmaResource> resources, FieldClass fieldClass, CsvConfig csvConfig
    ) {
        // TODO: Check for fine-grained field accessibility could be added here
        var validUmaFields = resources.stream().collect(
            Collectors.toMap(
                UmaResource::getUmaId, uma -> csvConfig.getFields(fieldClass, uma.getScopes())
            )
        );

        var publicFields = csvConfig.getPublicFields(fieldClass);

        return umaId -> {
            if (umaId == null) {
                Logger.warn(
                    "A resource was returned by the repository with no mapping from resource ID to UMA ID. Consider synchronizing.");
                return publicFields;
            }

            // TODO: Check for public study could be placed here?

            // Default Empty Set
            var fields = validUmaFields.getOrDefault(umaId, ImmutableSet.of());
            return Sets.union(fields, publicFields);
        };
    }

    /**
     * TODO: Delete
     *
     * From the UMA resource list and scopes obtain the list of resource IDs that can be safely processed for the resource type.
     *
     * @param umaResources the UMA resources and scopes.
     * @param umaScopes    the UMA scopes that the user must have access to for the resource, otherwise the resource is not considered.
     * @param umaIdGetter  function that returns the collection of resource IDs given the UMA ID.
     * @return the filtered collection of resource IDs.
     */
    public static List<String> filterFacetsOld(
        Collection<UmaResource> umaResources,
        Set<String> umaScopes,
        Function<String, Set<String>> umaIdGetter
    ) {
        return umaResources.stream()
            .filter(resource -> !Sets.intersection(umaScopes, resource.getScopes()).isEmpty())
            .map(resource -> umaIdGetter.apply(resource.getUmaId()))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     *
     * From the UMA resource list and scopes obtain the list of resource IDs that can be safely processed for the resource type.
     *
     * @param adcResources   the UMA resources and scopes.
     * @param umaScopes      the UMA scopes that the user must have access to for the resource, otherwise the resource is not considered.
     * @param resourceGetter function that returns the collection of resource IDs given an UMA ID.
     * @return the filtered collection of resource IDs.
     */
    public static List<String> filterFacets(
        Collection<AdcResource> adcResources,
        Set<String> umaScopes,
        Function<String, Set<String>> resourceGetter
    ) {
        return adcResources.stream()
            .filter(resource -> !Sets.intersection(umaScopes, resource.getUmaResource().getScopes()).isEmpty())
            .map(resource -> resourceGetter.apply(resource.getUmaResource().getUmaId()))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }
}
