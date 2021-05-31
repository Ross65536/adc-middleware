package pt.inesctec.adcauthmiddleware.db.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;

public interface AccessScopeRepository extends JpaRepository<AccessScope, Long> {
    /**
     * Retrieve all present AccessScopes names.
     *
     * @return String Set of ScopeAccess names present in the database
     */
    @Query("SELECT name from AccessScope")
    Set<String> findAllNames();

    List<AccessScope> findAllByOrderByIdAsc();
}
