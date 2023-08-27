package ru.valera.bot.exception;


import ru.valera.bot.client.TdApi;

public class TelegramClientTdApiException extends RuntimeException {

    private final TdApi.Error error;

    public TelegramClientTdApiException(String message) {
        super(message);
        this.error = null;
    }

    public TelegramClientTdApiException(String message, TdApi.Error error) {
        super(message);
        this.error = error;
    }

    /**
     * @return {@link TdApi.Error} error from TDLib or null.
     */
    public TdApi.Error getError() {
        return error;
    }

}
