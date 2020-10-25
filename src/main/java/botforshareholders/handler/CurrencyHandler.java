package botforshareholders.handler;

import botforshareholders.bot.Bot;
import botforshareholders.command.ParsedCommand;
import botforshareholders.model.Currency;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Scanner;

import static botforshareholders.Security.coinmarketcupTokenApi;
import static botforshareholders.util.CurrencyUtil.formatterBigDecimal;

@Component
public class CurrencyHandler extends AbstractHandler {
    private static final Logger log = LoggerFactory.getLogger(WeatherHandler.class);
    private static final Currency CURRENCY_MODEL = new Currency();

    public CurrencyHandler(Bot bot) {
        super(bot);
    }

    @Override
    public String operate(String chatId, ParsedCommand parsedCommand, Update update) {
        try {
            bot.sendQueue.add(getMessage(update.getMessage()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public SendMessage getMessage(Message message) throws IOException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(getCurrencyExchangeRate());
        return sendMessage;
    }

    private String getCurrencyExchangeRate() throws IOException {
        setToModelEURO();
        setToModelUSD();
        setToModelBTC();
        return CURRENCY_MODEL.toString();
    }

    private void setToModelUSD() throws IOException {
        URL urlUSD= new URL("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?id=2781&CMC_PRO_API_KEY=" + coinmarketcupTokenApi + "&convert=RUB");
        Scanner in = new Scanner((InputStream) urlUSD.getContent());
        StringBuilder result = new StringBuilder();

        while (in.hasNext()) {
            result.append(in.nextLine().replaceAll("_", " "));
        }

        JSONObject object = new JSONObject(result.toString());
        JSONObject objectData = object.getJSONObject("data");
        JSONObject object2781 = objectData.getJSONObject("2781");
        CURRENCY_MODEL.setNameUSD(object2781.getString("symbol"));

        JSONObject objectQuote = object2781.getJSONObject("quote");
        JSONObject objectUSD = objectQuote.getJSONObject("RUB");
        BigDecimal priseBtc = objectUSD.getBigDecimal("price");

        CURRENCY_MODEL.setPriceUSD(formatterBigDecimal(priseBtc.toString()));
    }

    private void setToModelEURO() throws IOException {
        URL urlUSD= new URL("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?id=2790&CMC_PRO_API_KEY=f082b7cc-f5c1-4daf-91ee-47304b232b9e&convert=RUB");
        Scanner in = new Scanner((InputStream) urlUSD.getContent());
        StringBuilder result = new StringBuilder();

        while (in.hasNext()) {
            result.append(in.nextLine().replaceAll("_", " "));
        }

        JSONObject object = new JSONObject(result.toString());
        JSONObject objectData = object.getJSONObject("data");
        JSONObject object2790 = objectData.getJSONObject("2790");
        CURRENCY_MODEL.setNameEuro(object2790.getString("symbol"));

        JSONObject objectQuote = object2790.getJSONObject("quote");
        JSONObject objectUSD = objectQuote.getJSONObject("RUB");
        BigDecimal priseBtc = objectUSD.getBigDecimal("price");

        CURRENCY_MODEL.setPriceEuro(formatterBigDecimal(priseBtc.toString()));
    }

    private void setToModelBTC() throws IOException {
        URL urlBTC = new URL("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest?id=1&CMC_PRO_API_KEY=" + coinmarketcupTokenApi);
        Scanner in = new Scanner((InputStream) urlBTC.getContent());
        StringBuilder result = new StringBuilder();

        while (in.hasNext()) {
            result.append(in.nextLine().replaceAll("_", " "));
        }

        JSONObject object = new JSONObject(result.toString());
        JSONObject objectData = object.getJSONObject("data");
        JSONObject object1 = objectData.getJSONObject("1");
        CURRENCY_MODEL.setNameBTC(object1.getString("symbol"));

        JSONObject objectQuote = object1.getJSONObject("quote");
        JSONObject objectUSD = objectQuote.getJSONObject("USD");
        BigDecimal priseBtc = objectUSD.getBigDecimal("price");
        BigDecimal volumeBtc = objectUSD.getBigDecimal("volume 24h");

        CURRENCY_MODEL.setPriceBTC(formatterBigDecimal(priseBtc.toString()));
        CURRENCY_MODEL.setVolume24BTC(formatterBigDecimal(volumeBtc.toString()));
    }
}