package pt.inesctec.adcauthmiddleware.config;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import pt.inesctec.adcauthmiddleware.utils.CollectionsUtils;


/**
 * Model for the configuration file. Middleware specific.
 */
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

  /**
   * File system path for the CSV configuration file with the fields configuration.
   * If not set the default CSV will be used.
   */
  private String adcCsvConfigPath;
  /**
   * Whether the facets are enabled from the user's perspective.
   */
  private boolean facetsEnabled = true;
  /**
   * Whether the unprotected public endpoints are enabled. Such as /v1 or /v1/info.
   */
  private boolean publicEndpointsEnabled = true;
  /**
   * Whether the "filters" ADC query parameter is enabled and usable by the user in POST requests.
   */
  private boolean adcFiltersEnabled = true;
  /**
   * Delayer pool size.
   * The number of request timings to memorize when delaying the permissions ticket emission.
   */
  @NotNull
  private Long requestDelaysPoolSize;
  /**
   * The "filters" operators that are blacklisted and therefore not usable by the user.
   * Format is a string: "op1,op2,etc"
   */
  private Set<String> filtersOperatorsBlacklist = Set.of();
  /**
   * The hash of the password used for the synchronize endpoint.
   * Must be a BCrypt hash with strength 10.
   */
  @NotNull @NotBlank
  private String synchronizePasswordHash;

  public String getAdcCsvConfigPath() {
    return adcCsvConfigPath;
  }

  public void setAdcCsvConfigPath(String adcCsvConfigPath) {
    this.adcCsvConfigPath = adcCsvConfigPath;
  }

  public String getSynchronizePasswordHash() {
    return synchronizePasswordHash;
  }

  public void setSynchronizePasswordHash(String synchronizePasswordHash) {
    this.synchronizePasswordHash = synchronizePasswordHash;
  }

  public boolean isFacetsEnabled() {
    return facetsEnabled;
  }

  public void setFacetsEnabled(boolean facetsEnabled) {
    this.facetsEnabled = facetsEnabled;
  }

  public boolean isPublicEndpointsEnabled() {
    return publicEndpointsEnabled;
  }

  public void setPublicEndpointsEnabled(boolean publicEndpointsEnabled) {
    this.publicEndpointsEnabled = publicEndpointsEnabled;
  }

  public boolean isAdcFiltersEnabled() {
    return adcFiltersEnabled;
  }

  public void setAdcFiltersEnabled(boolean adcFiltersEnabled) {
    this.adcFiltersEnabled = adcFiltersEnabled;
  }

  public Set<String> getFiltersOperatorsBlacklist() {
    return filtersOperatorsBlacklist;
  }

  private static final Set<String> AllOperators = Set.of("=", "!=", "<", "<=", ">", ">=", "is missing", "is", "is not missing", "not", "in", "exclude", "contains", "and", "or");

  public void setFiltersOperatorsBlacklist(String filtersOperatorsBlacklist) {
    var parts = filtersOperatorsBlacklist.split(",");
    this.filtersOperatorsBlacklist = Arrays.stream(parts)
        .map(String::trim)
        .filter(s -> ! s.equals(""))
        .collect(Collectors.toSet());

    Sets.SetView<String> diff = Sets.difference(this.filtersOperatorsBlacklist, AllOperators);
    if (! diff.isEmpty()) {
      throw new IllegalArgumentException("Invalid operators: " + CollectionsUtils.toString(diff));
    }
  }

  public long getRequestDelaysPoolSize() {
    return requestDelaysPoolSize;
  }

  public void setRequestDelaysPoolSize(long requestDelaysPoolSize) {
    if (requestDelaysPoolSize < 0) {
      throw new IllegalArgumentException("must be positive");
    }

    this.requestDelaysPoolSize = requestDelaysPoolSize;
  }
}
