package pt.inesctec.adcauthmiddleware.uma;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.uma.exceptions.TicketException;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

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
    public TicketException noRptToken(Collection<String> umaIds, Set<String> umaScopes)
            throws Exception {
        var umaResources =
                umaIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .map(id -> new UmaResource(id, umaScopes))
                        .toArray(UmaResource[]::new);

        return this.noRptToken(umaResources); // will throw
    }

}
