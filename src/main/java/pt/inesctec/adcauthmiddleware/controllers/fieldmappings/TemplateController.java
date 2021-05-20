package pt.inesctec.adcauthmiddleware.controllers.fieldmappings;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.inesctec.adcauthmiddleware.controllers.ResourceController;
import pt.inesctec.adcauthmiddleware.db.dto.PostTemplateDto;
import pt.inesctec.adcauthmiddleware.db.dto.PostTemplateMappingDto;
import pt.inesctec.adcauthmiddleware.db.dto.TemplateDto;
import pt.inesctec.adcauthmiddleware.db.dto.TemplatesListDto;
import pt.inesctec.adcauthmiddleware.db.models.AccessScope;
import pt.inesctec.adcauthmiddleware.db.models.TemplateMappings;
import pt.inesctec.adcauthmiddleware.db.models.Templates;
import pt.inesctec.adcauthmiddleware.db.repository.AccessScopeRepository;
import pt.inesctec.adcauthmiddleware.db.repository.TemplateMappingsRepository;
import pt.inesctec.adcauthmiddleware.db.repository.TemplatesRepository;
import pt.inesctec.adcauthmiddleware.uma.dto.internal.TokenIntrospection;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

@RestController
public class TemplateController extends ResourceController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private TemplatesRepository templatesRepository;
    @Autowired
    private TemplateMappingsRepository templateMappingsRepository;
    @Autowired
    private AccessScopeRepository accessScopeRepository;

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
    public ResponseEntity<List<TemplatesListDto>> templateList(
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);
        if (!introspection.isActive())
            throw new Exception("Access token is not active");
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
            @PathVariable Long templateId,
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);
        if (!introspection.isActive())
            throw new Exception("Access token is not active");
        Templates templateTemp = templatesRepository.findById(templateId).get();
        List<AccessScope> scopes = accessScopeRepository.findAll();
        TemplateDto template = new TemplateDto(templateTemp, scopes);
        return new ResponseEntity<>(template, HttpStatus.OK);
    }

    /**
     * Field Mappings for a Template.
     *
     * @return JSON of Template along with Field Mappings
     * @throws Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/templates",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostTemplateDto> templatePost(
            @RequestBody PostTemplateDto template,
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);
        if (!introspection.isActive())
            throw new Exception("Access token is not active");
        Templates new_template = templatesRepository.findByName(template.getName());
        if (new_template != null)
            throw new Exception("Template name already exists");
        Templates t = new Templates(template.getName());
        templatesRepository.save(t);
        new_template = templatesRepository.findByName(template.getName());
        for (PostTemplateMappingDto mapping:
             template.getMappings()) {
            long scope = mapping.getScope();
            for (Integer field:
                 mapping.getFields()) {
                templateMappingsRepository.saveMappings(new_template.getId(), field, scope);
            }
        }
        return new ResponseEntity<>(template, HttpStatus.OK);
    }

    /**
     * Delete a Template.
     *
     * @throws Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/templates/{templateId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> templateDelete(
            @PathVariable Long templateId,
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);
        if (!introspection.isActive())
            throw new Exception("Access token is not active");
        templatesRepository.deleteById(templateId);
        return new ResponseEntity<>("Deleted successfully", HttpStatus.OK);
    }
}
