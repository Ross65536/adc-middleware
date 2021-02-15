package pt.inesctec.adcauthmiddleware.uma;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.controllers.AdcAuthController;
import pt.inesctec.adcauthmiddleware.controllers.SpringUtils;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;
import pt.inesctec.adcauthmiddleware.utils.Delayer;

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
        var umaResources =
            umaIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .map(id -> new UmaResource(id, umaScopes))
            .toArray(UmaResource[]::new);

        return this.noRptToken(umaResources); // will throw
    }

    /**
     * Execute UMA workflow for multiple resources.
     * Emits a permissions ticket or returns the introspected RPT token resources.
     *
     * @param bearerToken    OIDC/UMA 2.0 Bearer Token (RPT)
     * @param umaIds         set of UMA ids for the requested resources
     * @param umaScopes      the scopes set for the request (for emitting permissions ticket).
     * @return the introspected RPT resources.
     * @throws Exception when emitting a permission ticket or an internal error occurs.
     */
    public List<UmaResource> execute(String bearerToken, Set<String> umaIds, Set<String> umaScopes) throws Exception {
        // empty scopes means public access, no UMA flow followed
        if (umaScopes.isEmpty()) {
            return ImmutableList.of();
        }

        if (bearerToken != null) {
            return this.umaClient.introspectToken(bearerToken, true).getPermissions();
        }

        if (umaIds.isEmpty()) {
            // when no resources return, just err
            throw SpringUtils.buildHttpException(HttpStatus.UNAUTHORIZED, null);
        }

        throw this.noRptToken(umaIds, umaScopes);
    }

    /**
     * Execute UMA workflow for a single resource ID.
     * Emits a permissions ticket or returns the introspected RPT token resources.
     *
     * @param bearerToken    OIDC/UMA 2.0 Bearer Token (RPT)
     * @param umaId          UMA id for the requested resource
     * @param umaScopes      the scopes set for the request (for emitting permissions ticket).
     * @return the introspected RPT resources.
     * @throws Exception when emitting a permission ticket or an internal error occurs.
     */
    public List<UmaResource> execute(String bearerToken, String umaId, Set<String> umaScopes) throws Exception {
        Set<String> umaIds = umaId != null ? Set.of(umaId) : Collections.<String>emptySet();
        return this.execute(bearerToken, umaIds, umaScopes);
    }

    /*public List<UmaResource> adcSingle(String bearerToken, String umaId, Set<String> umaScopes) throws Exception {
        if (umaId == null) {
            throw SpringUtils.buildHttpException(HttpStatus.NOT_FOUND, "Not found");
        }

        if (bearerToken != null) {
            return this.umaClient.introspectToken(bearerToken, true).getPermissions();
        }

        throw this.noRptToken(ImmutableList.of(umaId), umaScopes);
    }*/
}
