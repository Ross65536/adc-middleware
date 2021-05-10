package pt.inesctec.adcauthmiddleware.controllers.fieldmappings;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.inesctec.adcauthmiddleware.controllers.ResourceController;
import pt.inesctec.adcauthmiddleware.db.dto.*;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.repository.StudyMappingsRepository;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;
import pt.inesctec.adcauthmiddleware.uma.dto.UmaRegistrationResource;
import pt.inesctec.adcauthmiddleware.uma.dto.internal.TokenIntrospection;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class StudyController extends ResourceController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMappingsRepository studyMappingsRepository;

    /**
     * Protected by owner. List of studies.
     *
     * @return JSON list of Studies
     * @throws Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/study",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UmaRegistrationResource>> study(
            HttpServletRequest request
    ) throws Exception {
        List<UmaRegistrationResource> owned_resources = new ArrayList<>();
        String bearer = SpringUtils.getBearer(request);
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);
        String currentUserId = introspection.getUserId();
        String[] resources = umaClient.listUmaResources();
        for (String resourceId :
                resources) {
            UmaRegistrationResource resource = umaClient.getResource(resourceId);
            if (resource.getOwner().equals(currentUserId))
                owned_resources.add(resource);
        }
        return new ResponseEntity<>(owned_resources, HttpStatus.OK);
    }

    /**
     * Protected by owner. Field Mappings for a Study.
     *
     * @return JSON of Study along with Field Mappings
     * @throws Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/study/{studyId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudyDto> studySingle(
            @PathVariable String studyId,
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);
        String currentUserId = introspection.getUserId();
        String[] resources = umaClient.listUmaResources();
        for (String resourceId :
                resources) {
            UmaRegistrationResource resource = umaClient.getResource(resourceId);
            if (resource.getOwner().equals(currentUserId)) {
                StudyDto study = new StudyDto(studyRepository.findByUmaId(studyId));
                return new ResponseEntity<>(study, HttpStatus.OK);
            }
        }
        throw new Exception("Not the owner of this resource");
    }

    @RequestMapping(
            value = "/study/{studyId}",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> studySave(
            @PathVariable Long studyId,
            @RequestBody PostStudyDto study,
            HttpServletRequest request
    ) throws Exception {
        for (PostTemplateMappingDto mapping:
             study.getMappings()) {
            for (Integer fieldMapping:
                mapping.getFields()) {
                studyMappingsRepository.updateScope(mapping.getScope(), studyId, fieldMapping);
            }
        }
        return new ResponseEntity<>("Updated successfully", HttpStatus.OK);
    }
}
