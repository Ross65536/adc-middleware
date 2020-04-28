package pt.inesctec.adcauthmiddleware.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    @NonNull
    @NotBlank
    private String adcCsvConfigPath;

    public String getAdcCsvConfigPath() {
        return adcCsvConfigPath;
    }

    public void setAdcCsvConfigPath(String adcCsvConfigPath) {
        this.adcCsvConfigPath = adcCsvConfigPath;
    }
}
