package io.k8s.framework.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption; 
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Extractor {

    @Value("${data.city}")
    private String city;

    @Value("${data.zipcode}")
    private String zipcode;
    
    @Value("${data.station}")
    private String station;

    @Value("${data.gnome-conky}")
    private String baseDir;

    ObjectMapper mapper = YAMLMapper.builder()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .build();

    public String fetchMoonPhase() {
        Document doc;
        Format f = new SimpleDateFormat("MM/dd/yyyy");
        String strDate = f.format(new Date());
        StringBuilder moonPhaseBuilder = new StringBuilder();
        try {
            doc = Jsoup.connect("https://www.moongiant.com/phase/"+strDate+"/").get();
            Element moonPanel = doc.select("#today_").get(0);
            if (moonPanel.childNodes().get(4) instanceof TextNode) {
                moonPhaseBuilder.append(((TextNode) moonPanel.childNodes().get(4)).text());
            }

            String moonPhase = moonPhaseBuilder.toString();

            int percentage = Integer.parseInt(extractInt(moonPanel.select("span").text()));
            int divNumber = Math.round(percentage / 9);

            String moonImg = moonPhase.contains("Waxing") ? "WX-" + divNumber : moonPhase.contains("Waning") ? "WN-" + divNumber : moonPhase.contains("Full") ? "FULL" : "NEW";
            
            Path copied = Paths.get(baseDir + "/data/images/moon.png");
            Path originalPath = Paths.get(baseDir + "/images/moonPhases/" + moonImg + ".png");
            Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
            
            return moonPhase;
        } catch (Exception ex) {
            log.error("Failed to fetch moon phase", ex);
        }
        return "";
    }

    public void fetchAccuwatherPage() {
        Document doc;
        try {
            log.info("Getting weather data: " + city);
            doc = Jsoup.connect("https://www.accuweather.com/en/us/"+city+"/"+zipcode+"/daily-weather-forecast/"+station+"").get();

            WeatherModel[] weatherModels = extract5DayWeather(doc.selectFirst(".page-content.content-module").select(".daily-wrapper"));

            doc = Jsoup.connect("https://www.accuweather.com/en/us/"+city+"/"+zipcode+"/current-weather/"+station+"").get();
            
            WeatherModel currentWeather = extractCurrentWeather(doc.select(".current-weather-card.card-module.content-module ").first());
            int dayNight = doc.select(".half-day-card.content-module").size();
            WeatherModel nightWeather = extractNightWeather(doc.select(".half-day-card.content-module").last());

            WeatherStore weatherStore = WeatherStore.builder()
                .current(currentWeather)
                .day(dayNight > 1 ? extractDayWeather(doc.select(".half-day-card.content-module").first()) : nightWeather)
                .night(nightWeather)
                .one(weatherModels[0])
                .two(weatherModels[1])
                .three(weatherModels[2])
                .four(weatherModels[3])
                .five(weatherModels[4])
                .moonPhase(fetchMoonPhase())
                .metadata(Metadata.builder().lastUpdate(new SimpleDateFormat("EEEE dd-MMM-yy HH:mm:ssZ").format(new Date())).city(city).zipCode(zipcode).build())
                .build();
            mapper.writeValue(new File(baseDir + "/data/weatherStore"), weatherStore);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractInt(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    public WeatherModel extractCurrentWeather(Element element) {
        Elements panel = element.select(".current-weather-details.no-realfeel-phrase").last().select("div.detail-item.spaced-content");
        int panelSize = panel.size()-1;

        Path copied = Paths.get(baseDir + "/data/images/current.xml");
        try {
            Files.writeString(copied, element.select(".current-weather-info>svg").toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return WeatherModel.builder()
            .icon(element.select(".current-weather-info>svg").attr("data-src").split("/")[3])
            .temperature(extractInt(element.select(".display-temp").text()))
            .tempFeels(extractInt(panel.get(0).select("div").get(2).text()))
            .uvIndex(panelSize > 11 ? panel.get(panelSize-8).select("div").get(2).text() : "0")
            .wind(panelSize > 11 ? panel.get(panelSize-7).select("div").get(2).text(): "0")
            .condition(element.select(".phrase").text())
            .windGusts(panel.get(panelSize-6).select("div").get(2).text())
            .humidity(extractInt(panel.get(panelSize-5).select("div").get(2).text())+"%")
            .dewPoint(panel.get(panelSize-4).select("div").get(2).text())
            .pressure(panel.get(panelSize-3).select("div").get(2).text())
            .cloudCover(panel.get(panelSize-2).select("div").get(2).text())
            .visibility(panel.get(panelSize-1).select("div").get(2).text())
            .cloudCeiling(panel.get(panelSize).select("div").get(2).text())
            .build();
    }

    public WeatherModel extractDayWeather(Element element) {
        Path copied = Paths.get(baseDir + "/data/images/day.xml");
        try {
            Files.writeString(copied, element.select(".weather>svg").toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return WeatherModel.builder()
        .icon(element.select(".weather>svg").attr("data-src").split("/")[3])
        .temperature(extractInt(element.select(".temperature").text()))
        .tempFeels(extractInt(element.select(".real-feel>div").textNodes().get(0).text()))
        .condition(element.select(".phrase").text())
        .uvIndex(element.select(".panels>.left>p").get(0).select(".value").text())
        .wind(element.select(".panels>.left>p").get(1).select(".value").text())
        .windGusts(element.select(".panels>.left>p").get(2).select(".value").text())
        .precipitationProbability(element.select(".panels>.left>p").get(3).select(".value").text())
        .thunderstormProbability(element.select(".panels>.right>p").get(0).select(".value").text())
        .precipitation(element.select(".panels>.right>p").get(1).select(".value").text())
        .cloudCover(element.select(".panels>.right>p").get(2).select(".value").text())
        .build();
    }

    public WeatherModel extractNightWeather(Element element) {
        Path copied = Paths.get(baseDir + "/data/images/night.xml");
        try {
            Files.writeString(copied, element.select(".weather>svg").toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return WeatherModel.builder()
        .icon(element.select(".weather>svg").attr("data-src").split("/")[3])
        .temperature(extractInt(element.select(".temperature").text()))
        .tempFeels(extractInt(element.select(".real-feel>div").textNodes().get(0).text()))
        .condition(element.select(".phrase").text())
        .wind(element.select(".panels>.left>p").get(0).select(".value").text())
        .windGusts(element.select(".panels>.left>p").get(1).select(".value").text())
        .precipitationProbability(element.select(".panels>.left>p").get(2).select(".value").text())
        .thunderstormProbability(element.select(".panels>.right>p").get(0).select(".value").text())
        .precipitation(element.select(".panels>.right>p").get(1).select(".value").text())
        .cloudCover(element.select(".panels>.right>p").get(2).select(".value").text())
        .build();
    }

    public WeatherModel[] extract5DayWeather(Elements elements) {
        WeatherModel[] weatherModels = new WeatherModel[]{null, null, null, null, null};
        weatherModels[0] = extractExtendedWeather(0, elements.get(0));
        weatherModels[1] = extractExtendedWeather(1, elements.get(1));
        weatherModels[2] = extractExtendedWeather(2, elements.get(2));
        weatherModels[3] = extractExtendedWeather(3, elements.get(3));
        weatherModels[4] = extractExtendedWeather(4, elements.get(4));

        return weatherModels;
    }

    public WeatherModel extractExtendedWeather(int day, Element element) {
        Path copied = Paths.get(baseDir + "/data/images/"+ day +".xml");
        try {
            Files.writeString(copied, element.select(".info>svg").toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return WeatherModel.builder()
            .name(element.select(".info>.date>.module-header.dow.date").text())
            .condition(element.select(".phrase").text())
            .icon(element.select(".info>svg").attr("data-src").split("/")[3])
            .tempHigh(element.select(".temp>.high").text())
            .tempLow(element.select(".temp>.low").text())
            .wind(element.select(".right>.panel-item").select(".value").text())
            .build();
    }
}
