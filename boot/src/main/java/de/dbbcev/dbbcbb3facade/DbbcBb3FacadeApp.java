package de.dbbcev.dbbcbb3facade;

import de.dbbcev.dbbcbb3facade.config.DbbcBb3Config;
import de.dbbcev.dbbcbb3facade.domain.StatusRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan(basePackageClasses = DbbcBb3Config.class)
@EnableMongoRepositories(basePackageClasses = {StatusRepository.class})
@SpringBootApplication
public class DbbcBb3FacadeApp {

    public static void main(String[] args) {
        SpringApplication.run(DbbcBb3FacadeApp.class, args);
    }

}
