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
import pt.inesctec.adcauthmiddleware.db.dto.TemplateDto;
import pt.inesctec.adcauthmiddleware.db.dto.TemplatesListDto;
import pt.inesctec.adcauthmiddleware.db.models.Templates;
import pt.inesctec.adcauthmiddleware.db.repository.TemplatesRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TemplateController extends ResourceController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private TemplatesRepository templatesRepository;

    /**
     * Field Mappings for a Template.
     *
     * @return JSON list of Templates
     * @throws Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/templates",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TemplatesListDto>> templateList() throws Exception {
        List<Templates> templates = templatesRepository.findAll();
        List<TemplatesListDto> templateList = templates.stream()
                .map(TemplatesListDto::new)
                .collect(Collectors.toList());
        return new ResponseEntity<>(templateList, HttpStatus.OK);
    }

    /**
     * Field Mappings for a Template.
     *
     * @return JSON of Template along with Field Mappings
     * @throws Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/templates/{templateId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TemplateDto> templateSingle(
            @PathVariable Long templateId
    ) throws Exception {
        TemplateDto template = new TemplateDto(templatesRepository.findById(templateId).get());
        return new ResponseEntity<>(template, HttpStatus.OK);
    }
}
