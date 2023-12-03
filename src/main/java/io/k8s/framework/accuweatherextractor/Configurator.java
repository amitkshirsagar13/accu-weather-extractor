package io.k8s.framework.accuweatherextractor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.k8s.framework.extractor.Extractor;

@Configuration
public class Configurator {
    @Bean
    public Extractor extractor() {
        return new Extractor();
    }
}
