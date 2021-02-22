package pt.inesctec.adcauthmiddleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;

/**
 * Spring's 'entrypoint'.
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@EnableConfigurationProperties(AdcConfiguration.class)
@EnableCaching
public class AdcAuthMiddlewareApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdcAuthMiddlewareApplication.class, args);
    }
}
