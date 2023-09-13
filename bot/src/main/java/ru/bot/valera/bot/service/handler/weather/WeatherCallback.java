package ru.bot.valera.bot.service.handler.weather;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.bot.valera.bot.model.Command;
import ru.bot.valera.bot.model.Content;
import ru.bot.valera.bot.model.weather.CurrentWeather;
import ru.bot.valera.bot.model.weather.WeatherSevenDays;
import ru.bot.valera.bot.service.handler.AbstractContent;
import ru.bot.valera.bot.service.keyboards.weathher.CallBackWeatherKeyboardEightDay;
import ru.bot.valera.bot.service.keyboards.weathher.CallBackWeatherKeyboardOneDay;
import ru.bot.valera.bot.service.keyboards.weathher.CallBackWeatherKeyboardStart;
import ru.bot.valera.bot.to.UpdateTO;
import ru.bot.valera.bot.util.JsonUtil;

import java.util.Set;

import static ru.bot.valera.bot.model.Command.EDIT_WEATHER_KEYBOARD;
import static ru.bot.valera.bot.util.WeatherUtil.CITIES_NAMES;
import static ru.bot.valera.bot.util.WeatherUtil.coordsList;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeatherCallback extends AbstractContent {

    @Value("${tokens.weather_token_api}" )
    String weatherToken;

    final CallBackWeatherKeyboardStart callBackWeatherKeyboardStart;
    final CallBackWeatherKeyboardOneDay callBackWeatherKeyboardOneDay;
    final CallBackWeatherKeyboardEightDay callBackWeatherKeyboardEightDay;

    @Override
    public Content handle(UpdateTO updateTO) {
        String[] callBackData = updateTO.getCallBackData();

        switch (callBackData[0]) {
            case "1" -> {
                return getContent(updateTO, callBackWeatherKeyboardOneDay);
            }
            case "2" -> {
                return getContent(updateTO, callBackWeatherKeyboardEightDay);
            }
            case "3" -> {
                return getContent(updateTO, callBackWeatherKeyboardStart);
            }
            default -> {
                updateTO.setCommand(Command.DEFAULT);
                return getContent(getWeather(updateTO.getCallBackData()), updateTO);
            }
        }
    }

    public String getWeather(String[] callBackData) {
        String message = "";
        switch (callBackData[0]) {
            case "4" -> message = getWeatherForOneDay(callBackData[1]);
            case "5" -> message = getWeatherForSevenDay(callBackData[1], callBackData[2]);
        }
        return message;
    }

    public String getWeatherForOneDay(String cityIdx) {
        String city = CITIES_NAMES.get(coordsList.get(Integer.parseInt(cityIdx)));
        CurrentWeather currentWeather = JsonUtil.readValue("https://api.openweathermap.org/data/2.5/weather?q=" + city + "&units=metric&appid=" +
                weatherToken + "&lang=ru", CurrentWeather.class);
        if (currentWeather == null) {
            return "Сервис погода недоступен. Чиним.";
        }
        return currentWeather.toString();
    }

    public String getWeatherForSevenDay(String lat, String lon) {
        WeatherSevenDays weatherSevenDays = JsonUtil.readValue("https://api.openweathermap.org/data/2.5/onecall?lat=" + lat + "&lon=" + lon +
                "&exclude=hourly,current&units=metric&appid=" + weatherToken + "&lang=ru", WeatherSevenDays.class);

        if (weatherSevenDays == null) {
            return "Сервис погода недоступен. Чиним.";
        }
        weatherSevenDays.setLat(lat);
        weatherSevenDays.setLon(lon);
        return weatherSevenDays.toString();
    }

    @Override
    public Set<Command> getMessageType() {
        return Set.of(EDIT_WEATHER_KEYBOARD);
    }
}
