package pt.inesctec.adcauthmiddleware.controllers.fieldmappings;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pt.inesctec.adcauthmiddleware.controllers.ResourceController;
import pt.inesctec.adcauthmiddleware.db.dto.AdcFieldsDto;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;
import pt.inesctec.adcauthmiddleware.db.repository.AdcFieldsRepository;

import java.util.ArrayList;
import java.util.List;

@RestController
public class AdcFieldsController extends ResourceController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private AdcFieldsRepository adcFieldsRepository;

    /**
     * Adc Fields.
     *
     * @return JSON list of Adc Fields
     * @throw Exception for connection failures, authentication failure
     */
    @RequestMapping(
            value = "/fields",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AdcFieldsDto>> fieldsList() throws Exception {
        List<AdcFields> fields = adcFieldsRepository.findAll();
        List<AdcFieldsDto> fieldList = new ArrayList<>();
        for (AdcFields adcField : fields) {
            fieldList.add(new AdcFieldsDto(adcField));
        }
        return new ResponseEntity<>(fieldList, HttpStatus.OK);
    }
}
