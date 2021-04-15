package pt.inesctec.adcauthmiddleware.db.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class AdcFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "id_type", nullable = false)
    private AdcFieldType type;

    @Column()
    private String prefix;

    protected AdcFields() {}

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

    public AdcFieldType getType() {
        return type;
    }

    public void setType(AdcFieldType type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Auxiliary function to check if name is present in a list of {@link AdcFields}.
     *
     * @return true if present
     */
    @Transient
    public static boolean listContains(List<AdcFields> list, String name) {
        return list.stream().map(AdcFields::getName).anyMatch(name::equals);
    }

    /**
     * Get a List of names from a list of {@link AdcFields}.
     *
     * @return List of {@link AdcFields} names
     */
    public static List<String> toNameList(List<AdcFields> adcFields) {
        return adcFields.stream().map(AdcFields::getName).collect(Collectors.toList());
    }
}
