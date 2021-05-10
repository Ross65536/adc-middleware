package pt.inesctec.adcauthmiddleware.controllers.fieldmappings;

import java.util.ArrayList;
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
import pt.inesctec.adcauthmiddleware.db.dto.AdcFieldsDto;
import pt.inesctec.adcauthmiddleware.db.models.AdcFields;
import pt.inesctec.adcauthmiddleware.db.repository.AdcFieldsRepository;
import pt.inesctec.adcauthmiddleware.uma.dto.internal.TokenIntrospection;
import pt.inesctec.adcauthmiddleware.utils.SpringUtils;

import javax.servlet.http.HttpServletRequest;

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
    public ResponseEntity<List<AdcFieldsDto>> fieldsList(
            HttpServletRequest request
    ) throws Exception {
        String bearer = SpringUtils.getBearer(request);
        TokenIntrospection introspection = umaClient.introspectToken(bearer, false);
        if (!introspection.isActive())
            throw new Exception("Access token is not active");
        List<AdcFields> fields = adcFieldsRepository.findAll();
        List<AdcFieldsDto> fieldList = fields.stream()
                .map(adcFields -> new AdcFieldsDto(adcFields))
                .collect(Collectors.toList());
        return new ResponseEntity<>(fieldList, HttpStatus.OK);
    }
}
