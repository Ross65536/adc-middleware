package pt.inesctec.adcauthmiddleware.controllers;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import pt.inesctec.adcauthmiddleware.adc.AdcClient;
import pt.inesctec.adcauthmiddleware.config.AppConfig;
import pt.inesctec.adcauthmiddleware.db.repository.StudyMappingsRepository;
import pt.inesctec.adcauthmiddleware.db.services.DbService;

public class BenchmarkingController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(BenchmarkingController.class);

    @Autowired
    protected StudyMappingsRepository studyMappingsRepository;

    @Autowired
    protected AppConfig appConfig;

    @Autowired
    protected AdcClient adcClient;

    @Autowired
    protected DbService dbService;

    @GetMapping(value = "/test")
    public void testRoute() {
    }
}
