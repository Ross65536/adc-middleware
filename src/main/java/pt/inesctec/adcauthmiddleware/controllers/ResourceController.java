package pt.inesctec.adcauthmiddleware.controllers;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.db.dto.TemplateDTO;
import pt.inesctec.adcauthmiddleware.db.dto.TemplatesListDTO;
import pt.inesctec.adcauthmiddleware.db.models.Templates;
import pt.inesctec.adcauthmiddleware.db.repository.TemplatesRepository;

@RequestMapping("/resource")
@RestController
public class ResourceController {
    @Autowired
    protected AppConfig appConfig;

    @Autowired
    protected TemplatesRepository templatesRepository;

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdcAuthController.class);

    /**
     * Field Mappings for a Study
     *
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(
        value = "/templates",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TemplatesListDTO>> templateList(HttpServletRequest request) throws Exception {
        List<Templates> templates = templatesRepository.findAll();
        List<TemplatesListDTO> templateList = templates.stream()
            .map(template -> {
                return new TemplatesListDTO(template);
            }).collect(Collectors.toList());
        return new ResponseEntity<>(templateList, HttpStatus.OK);
    }

    /**
     * Field Mappings for a Study
     *
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(
        value = "/templates/{templateId}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TemplateDTO> templateSingle(
        HttpServletRequest request,
        @PathVariable Long templateId
    ) throws Exception {
        TemplateDTO template = new TemplateDTO(templatesRepository.findById(templateId).get());
        return new ResponseEntity<>(template, HttpStatus.OK);
    }
}
