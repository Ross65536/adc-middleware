package pt.inesctec.adcauthmiddleware.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.inesctec.adcauthmiddleware.config.csv.CsvField;
import pt.inesctec.adcauthmiddleware.config.csv.FieldClass;
import pt.inesctec.adcauthmiddleware.config.csv.FieldType;
import pt.inesctec.adcauthmiddleware.config.csv.IncludeField;
import pt.inesctec.adcauthmiddleware.db.repository.AccessScopeRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;

@Component
public class FieldConfig {
    @Autowired
    AccessScopeRepository accessScopeRepository;

    @Autowired
    StudyRepository studyRepository;

    /**
     * Get the complete set of scopes present in the database, independently of the resource.
     *
     * @return Set of scopes
     */
    public Set<String> getAllUmaScopes() {
        return accessScopeRepository.findAllNames();
    }

    /**
     * Get the complete set of scopes that correspond to a specific resource.
     *
     * @param umaId the resource type
     * @return Set of scopes of resource with umaId
     */
    public Set<String> getUmaScopes(String umaId) {
        return Set.of("");
    }
}
