package pt.inesctec.adcauthmiddleware.db.dto;

import pt.inesctec.adcauthmiddleware.db.models.AccessScope;

public class AccessScopeDto {
    private int id;
    private String name;

    public AccessScopeDto(AccessScope scope) {
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
