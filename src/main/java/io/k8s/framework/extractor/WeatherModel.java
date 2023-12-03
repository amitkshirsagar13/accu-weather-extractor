package io.k8s.framework.extractor;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class WeatherModel {
    private String name;
    private String icon;
    private String temperature;
    private String tempHigh;
    private String tempLow;
    private String tempFeels;
    private String uvIndex;
    private String humidity;
    private String visibility;
    private String condition;
    private String cloudCover;
    private String cloudCeiling;
    private String pressure;
    private String dewPoint;
    private String wind;
    private String windGusts;
    private String precipitation;
    private String precipitationProbability;
    private String thunderstormProbability;
    
}
