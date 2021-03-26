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
