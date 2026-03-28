package com.schemenavigator;

import com.schemenavigator.config.AuthUiProperties;
import com.schemenavigator.config.DatasetImportProperties;
import com.schemenavigator.config.ReminderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({DatasetImportProperties.class, AuthUiProperties.class, ReminderProperties.class})
public class SchemeNavigatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemeNavigatorApplication.class, args);
    }
}
