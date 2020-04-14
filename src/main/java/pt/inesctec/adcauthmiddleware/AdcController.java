package pt.inesctec.adcauthmiddleware;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdcController {

    @Autowired
    private AdcConfiguration config;

    @GetMapping("/study/{studyId}/repertoire/{repertoireId}")
    public String greeting(@PathVariable String studyId, @PathVariable String repertoireId) {
        return studyId + " " + config.getResourceServerUrl();
    }
}
