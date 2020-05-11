package pt.inesctec.adcauthmiddleware.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String adcCsvConfigPath;

    @NotNull
    @NotBlank
    private String synchronizePasswordHash;

    public String getAdcCsvConfigPath() {
        return adcCsvConfigPath;
    }

    public void setAdcCsvConfigPath(String adcCsvConfigPath) {
        this.adcCsvConfigPath = adcCsvConfigPath;
    }

    public String getSynchronizePasswordHash() {
        return synchronizePasswordHash;
    }


    public void setSynchronizePasswordHash(String synchronizePasswordHash) {
        this.synchronizePasswordHash = synchronizePasswordHash;
    }
}

