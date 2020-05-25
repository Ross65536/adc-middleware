package pt.inesctec.adcauthmiddleware.uma.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenIntrospection {
  private boolean active;
  private List<UmaResource> permissions;

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<UmaResource> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<UmaResource> permissions) {
    this.permissions = permissions;
  }
}
