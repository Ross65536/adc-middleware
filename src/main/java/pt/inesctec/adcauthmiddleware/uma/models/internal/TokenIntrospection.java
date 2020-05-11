package pt.inesctec.adcauthmiddleware.uma.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenIntrospection {
  private boolean active;
  private long exp;
  private List<UmaResource> permissions;

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public long getExp() {
    return exp;
  }

  public void setExp(long exp) {
    this.exp = exp;
  }

  public List<UmaResource> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<UmaResource> permissions) {
    this.permissions = permissions;
  }
}
