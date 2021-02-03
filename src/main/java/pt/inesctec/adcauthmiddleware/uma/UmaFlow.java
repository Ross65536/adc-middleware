package pt.inesctec.adcauthmiddleware.uma;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.adc.models.AdcSearchRequest;
import pt.inesctec.adcauthmiddleware.controllers.SpringUtils;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;
import pt.inesctec.adcauthmiddleware.utils.Delayer;
import pt.inesctec.adcauthmiddleware.utils.ThrowingFunction;

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
     * The common UMA flow for POST endpoints. Emits a permissions ticket or returns the introspected RPT token resources.
     *
     * @param request        the user request
     * @param umaIds         set of UMA ids for the requested resources
     * @param delayer        the delayer to make all requests take the same time.
     * @param umaScopes      the scopes set for the request (for emitting permissions ticket).
     * @return the introspected RPT resources.
     * @throws Exception when emitting a permission ticket or an internal error occurs.
     */
    public List<UmaResource> adcSearch(
        HttpServletRequest request,
        Set<String> umaIds,
        Delayer delayer,
        Set<String> umaScopes)
        throws Exception {
        var startTime = LocalDateTime.now();

        // empty scopes means public access, no UMA flow followed
        if (umaScopes.isEmpty()) {
            return ImmutableList.of();
        }

        var bearer = SpringUtils.getBearer(request);

        if (bearer != null) {
            return this.umaClient.introspectToken(bearer, true).getPermissions();
        }

        // TODO: Why the delay...?
        //delayer.delay(startTime);

        if (umaIds.isEmpty()) {
            // when no resources return, just err
            throw SpringUtils.buildHttpException(HttpStatus.UNAUTHORIZED, null);
        }

        throw this.noRptToken(umaIds, umaScopes);
    }

}
