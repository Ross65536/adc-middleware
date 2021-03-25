package pt.inesctec.adcauthmiddleware.uma;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.dto.UmaResource;

/**
 * Responsible for the UMA flow.
 */
@Component
public class UmaFlow {
    private final UmaClient umaClient;

    public UmaFlow(UmaClient umaClient) {
        this.umaClient = umaClient;
    }

    /**
     * Throws permissions ticket. Used when no RPT token is provided for resources.
     *
     * @param resources the UMA resources for throwing.
     * @return the ticket exception
     * @throws Exception when internal error occurs.
     */
    public TicketException noRptToken(UmaResource[] resources) throws Exception {
        var ticket = this.umaClient.requestPermissionsTicket(resources);
        return new TicketException(ticket, this.umaClient.getIssuer());
    }

    /**
     * Overload for {@link #noRptToken(UmaResource[])}.
     *
     * @param umaIds    the UMA resource IDs.
     * @param umaScopes the UMA scopes applied to all of the UMA resources.
     * @return the ticket exception
     * @throws Exception on internal error.
     */
    public TicketException noRptToken(Collection<String> umaIds, Set<String> umaScopes) throws Exception {
        var umaResources = umaIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .map(id -> new UmaResource(id, umaScopes))
            .toArray(UmaResource[]::new);

        return this.noRptToken(umaResources); // will throw
    }

    /**
     * Execute UMA workflow for multiple resources:
     * <ul>
     *     <li>Emits a permissions ticket if no token is provided;</li>
     *     <li>If a permission ticket is provided, returns the introspected RPT token resources:</li>
     * </ul>
     * The introspected RPT token resources represent the list of resources the user has access to,
     * along with the scopes the user has access to.
     *
     * @param bearerToken    OIDC/UMA 2.0 Bearer Token (RPT)
     * @param umaIds         Set of UMA ids for the requested resources
     * @param umaScopes      The scopes to check for accessibility
     * @return the introspected RPT resources.
     * @throws Exception when emitting a permission ticket or an internal error occurs.
     */
    public List<UmaResource> execute(String bearerToken, Set<String> umaIds, Set<String> umaScopes) throws Exception {
        // Empty scopes means public access, no UMA flow followed
        if (umaScopes.isEmpty()) {
            return ImmutableList.of();
        }

        if (bearerToken != null) {
            return this.umaClient.introspectToken(bearerToken, true).getPermissions();
        }

        // When no resources return, just err
        if (umaIds.isEmpty()) {
            throw SpringUtils.buildHttpException(HttpStatus.UNAUTHORIZED, null);
        }

        // When no token is provided
        throw this.noRptToken(umaIds, umaScopes);
    }

    /**
     * Execute UMA workflow for a single resource ID.
     * <ul>
     *     <li>Emits a permissions ticket if no token is provided;</li>
     *     <li>If a permission ticket is provided, returns the introspected RPT token resources</li>
     * </ul>
     * The introspected RPT token resources represent the list of resources the user has access to,
     * along with the scopes the user has access to.
     *
     * @param bearerToken    OIDC/UMA 2.0 Bearer Token (RPT)
     * @param umaId          UMA id for the requested resource
     * @param umaScopes      The scopes to check for accessibility
     * @return the introspected RPT resources.
     * @throws Exception when emitting a permission ticket or an internal error occurs.
     */
    public List<UmaResource> execute(String bearerToken, String umaId, Set<String> umaScopes) throws Exception {
        Set<String> umaIds = umaId != null ? Set.of(umaId) : Collections.<String>emptySet();
        return this.execute(bearerToken, umaIds, umaScopes);
    }
}
