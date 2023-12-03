package io.k8s.framework.accuweatherextractor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.k8s.framework.extractor.Extractor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class AccuWeatherExtractorApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(AccuWeatherExtractorApplication.class, args);
	}

    @Override
    public void run(String... args) throws Exception {
		log.info("first command-line parameter: '{}'");
		extractor.fetchAccuwatherPage();
    }

	@Autowired
	private Extractor extractor;
}
