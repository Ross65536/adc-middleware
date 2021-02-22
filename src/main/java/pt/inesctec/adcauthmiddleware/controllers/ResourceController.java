package pt.inesctec.adcauthmiddleware.controllers;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.db.models.Templates;
import pt.inesctec.adcauthmiddleware.db.repository.TemplatesRepository;

@RequestMapping("/resource")
@RestController
public class ResourceController {
    @Autowired
    protected AppConfig appConfig;

    @Autowired
    protected TemplatesRepository templates;

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
    public ResponseEntity<List<Templates>> repertoire(HttpServletRequest request) throws Exception {
        return new ResponseEntity<>(templates.findAll(), HttpStatus.OK);
    }
}
