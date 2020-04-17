package pt.inesctec.adcauthmiddleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import pt.inesctec.adcauthmiddleware.config.AdcConfiguration;

@SpringBootApplication
@EnableConfigurationProperties(AdcConfiguration.class)
public class AdcAuthMiddlewareApplication {

  public static void main(String[] args) {
    SpringApplication.run(AdcAuthMiddlewareApplication.class, args);
  }
}
