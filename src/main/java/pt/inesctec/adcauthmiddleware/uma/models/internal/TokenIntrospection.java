package pt.inesctec.adcauthmiddleware.uma.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import pt.inesctec.adcauthmiddleware.uma.models.UmaResource;

import java.util.List;

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
