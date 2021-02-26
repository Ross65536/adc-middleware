package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.AccessScope;

public class AccessScopeDTO {
    private int id;
    private String name;

    public AccessScopeDTO(AccessScope scope) {
        this.id = scope.getId();
        this.name = scope.getName();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
