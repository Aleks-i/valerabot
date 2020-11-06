package botforshareholders.handler;

import botforshareholders.bot.Bot;
import botforshareholders.command.Command;
import botforshareholders.command.ParsedCommand;
import botforshareholders.model.WeatherNow;
import botforshareholders.keyboard.WeatherKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static botforshareholders.command.Parser.getWordsFromMessage;

@Component
public class WeatherHandler extends AbstractHandler {
    private static final Logger log = LoggerFactory.getLogger(WeatherHandler.class);

    private final WeatherKeyboard weatherKeyboard;

    public WeatherHandler(Bot bot, WeatherNow weatherModelNow, WeatherKeyboard weatherKeyboard) {
        super(bot);
        this.weatherKeyboard = weatherKeyboard;
    }

    @Override
    public String operate(String chatId, ParsedCommand parsedCommand, Update update) {
        Command command = parsedCommand.getCommand();
        String text = update.getMessage().getText();

        if (text.equalsIgnoreCase("погода")) {
            Bot.sendQueue.add(getMessageWithKeyboardWeatherOneDay(update.getMessage()));
        }
        return "";
    }

    public SendMessage getMessageWithKeyboardWeatherOneDay(Message message) {
        List<String> words = getWordsFromMessage(message.getText());
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());
        weatherKeyboard.setInlineKeyboardMarkupWeather(sendMessage);
        return sendMessage.setText("Выбери чо нить");
    }
}