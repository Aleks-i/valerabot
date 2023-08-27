package ru.valera.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import ru.valera.bot.client.Client;
import ru.valera.bot.client.TdApi;
import ru.valera.bot.client.TelegramClient;
import ru.valera.bot.client.updates.ClientAuthorizationState;
import ru.valera.bot.client.updates.ClientAuthorizationStateImpl;
import ru.valera.bot.client.updates.UpdateAuthorizationNotification;
import ru.valera.bot.client.updates.UpdateNotificationListener;
import ru.valera.bot.properties.TelegramProperties;

import java.util.Collection;

@Slf4j
@Configuration
@ConfigurationPropertiesScan(basePackages = "ru.valera.bot.properties")
public class TelegramClientAutoConfiguration {

    //Loading TDLib library
    static {
        try {
            String os = System.getProperty("os.name");
            if (os != null && os.toLowerCase().startsWith("windows")) {
                System.loadLibrary("libcrypto-1_1-x64");
                System.loadLibrary("libssl-1_1-x64");
                System.loadLibrary("zlib1");
            }
            System.loadLibrary("tdjni");
        } catch (UnsatisfiedLinkError e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Autoconfigured telegram client.
     *
     * @param properties               {@link TelegramProperties}
     * @param notificationHandlers     collection of {@link UpdateNotificationListener} beans
     * @param defaultHandler           default handler for incoming updates
     * @param clientAuthorizationState authorization state of the client
     * @return {@link TelegramClient}
     */
    @Bean
    public TelegramClient telegramClient(TelegramProperties properties,
                                         Collection<UpdateNotificationListener<?>> notificationHandlers,
                                         Client.ResultHandler defaultHandler,
                                         ClientAuthorizationState clientAuthorizationState) {
        return new TelegramClient(properties, notificationHandlers, defaultHandler, clientAuthorizationState);
    }

    /**
     * Client authorization state.
     *
     * @return {@link ClientAuthorizationState}
     */
    @Bean
    public ClientAuthorizationState clientAuthorizationState() {
        return new ClientAuthorizationStateImpl();
    }

    /**
     * Notification listener for authorization sate change.
     *
     * @return {@link UpdateNotificationListener<TdApi.UpdateAuthorizationState>}
     */
    @Bean
    public UpdateNotificationListener<TdApi.UpdateAuthorizationState> updateAuthorizationNotification(TelegramProperties properties,
                                                                                                      @Lazy TelegramClient telegramClient,
                                                                                                      ClientAuthorizationState clientAuthorizationState) {
        return new UpdateAuthorizationNotification(properties, telegramClient, clientAuthorizationState);
    }

    /**
     * @return Default handler for incoming TDLib updates.
     * Could be overwritten by another bean
     */
    @Bean
    public Client.ResultHandler defaultHandler() {
        return (TdApi.Object object) ->
                log.debug("\nSTART DEFAULT HANDLER\n" +
                        object.toString() + "\n" +
                        "END DEFAULT HANDLER");
    }

}