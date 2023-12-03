package io.k8s.framework.extractor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherStore {
    private WeatherModel current;
    private WeatherModel day;
    private WeatherModel night;
    private WeatherModel one;
    private WeatherModel two;
    private WeatherModel three;
    private WeatherModel four;
    private WeatherModel five;
    private Metadata metadata;
    
    private String moonPhase;
}
