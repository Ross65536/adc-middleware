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

@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

  private String adcCsvConfigPath;
  private boolean facetsEnabled = true;
  private boolean publicEndpointsEnabled = true;
  private boolean adcFiltersEnabled = true;
  private Set<String> filtersOperatorsBlacklist = Set.of();
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
        .collect(Collectors.toSet());

    Sets.SetView<String> diff = Sets.difference(this.filtersOperatorsBlacklist, AllOperators);
    if (! diff.isEmpty()) {
      throw new IllegalArgumentException("Invalid operators: " + CollectionsUtils.toString(diff));
    }
  }
}
