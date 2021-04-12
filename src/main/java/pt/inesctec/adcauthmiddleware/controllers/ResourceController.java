package pt.inesctec.adcauthmiddleware.controllers;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import pt.inesctec.adcauthmiddleware.config.AppConfig;

@RequestMapping("/resource")
public abstract class ResourceController {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    protected AppConfig appConfig;
}
