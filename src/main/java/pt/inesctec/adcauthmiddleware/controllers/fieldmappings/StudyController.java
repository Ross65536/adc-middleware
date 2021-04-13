package pt.inesctec.adcauthmiddleware.controllers.fieldmappings;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.controllers.ResourceController;
import pt.inesctec.adcauthmiddleware.db.dto.AdcFieldsDto;
import pt.inesctec.adcauthmiddleware.db.dto.StudyDto;
import pt.inesctec.adcauthmiddleware.db.dto.StudyListDto;
import pt.inesctec.adcauthmiddleware.db.models.Study;
import pt.inesctec.adcauthmiddleware.db.repository.StudyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class StudyController extends ResourceController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private StudyRepository studyRepository;

    /**
     * Field Mappings for a Study.
     *
     * @return JSON list of Studies
     * @throws Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/study",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StudyListDto>> studiesList() throws Exception {
        List<Study> studies = studyRepository.findAll();
        List<StudyListDto> studyList = studies.stream()
                .map(study -> new StudyListDto(study))
                .collect(Collectors.toList());
        return new ResponseEntity<>(studyList, HttpStatus.OK);
    }

    /**
     * Field Mappings for a Study.
     *
     * @return JSON of Study along with Field Mappings
     * @throws Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/study/{studyId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudyDto> studySingle(
            @PathVariable String studyId
    ) throws Exception {
        StudyDto study = new StudyDto(studyRepository.findByStudyId(studyId));
        return new ResponseEntity<>(study, HttpStatus.OK);
    }
}
