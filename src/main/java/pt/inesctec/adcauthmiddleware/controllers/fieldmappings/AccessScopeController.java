package pt.inesctec.adcauthmiddleware.controllers.fieldmappings;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.controllers.ResourceController;
import pt.inesctec.adcauthmiddleware.db.dto.AccessScopeDto;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.repository.AccessScopeRepository;

@RestController
public class AccessScopeController extends ResourceController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private AccessScopeRepository accessScopeRepository;

    /**
     * Access scopes
     *
     * @return JSON list of Access Scopes
     * @throw Exception for connection failures, authentication failures
     */
    @RequestMapping(
            value = "/scopes",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccessScopeDto>> scopesList() throws Exception {
        List<AccessScope> scopes = accessScopeRepository.findAll();
        List<AccessScopeDto> scopeList = scopes.stream()
                .map(AccessScopeDto::new)
                .collect(Collectors.toList());
        return new ResponseEntity<>(scopeList, HttpStatus.OK);
    }
}
