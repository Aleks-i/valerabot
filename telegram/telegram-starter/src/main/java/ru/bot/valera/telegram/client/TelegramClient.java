package ru.bot.valera.telegram.client;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import ru.bot.valera.telegram.client.updates.ClientAuthorizationState;
import ru.bot.valera.telegram.client.updates.UpdateNotificationListener;
import ru.bot.valera.telegram.exception.TelegramClientConfigurationException;
import ru.bot.valera.telegram.exception.TelegramClientTdApiException;
import ru.bot.valera.telegram.properties.TelegramProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class TelegramClient {

    private final Client client;

    private final Client.ResultHandler defaultHandler;

    private final ClientAuthorizationState clientAuthorizationState;

    /**
     * @param properties               TDlib client properties
     * @param notificationHandlers     registered notifications handlers
     * @param defaultHandler           default handler for unhandled events
     * @param clientAuthorizationState authorization state of the client
     */
    public TelegramClient(TelegramProperties properties,
                          Collection<UpdateNotificationListener<?>> notificationHandlers,
                          Client.ResultHandler defaultHandler,
                          ClientAuthorizationState clientAuthorizationState) {
        this.defaultHandler = defaultHandler;
        checkProperties(properties);
        this.clientAuthorizationState = clientAuthorizationState;
        this.client = initializeNativeClient(properties, notificationHandlers);
    }

    private void checkProperties(TelegramProperties properties) {
        if (properties.phone() == null) {
            throw new TelegramClientConfigurationException("The phone number of the user not filled. " +
                    "Specify property spring.telegram.client.phone" );
        }
        if (properties.apiId() == 0) {
            throw new TelegramClientConfigurationException("Application identifier for Telegram API access is invalid. " +
                    "Specify property spring.telegram.client.api-id" );
        }
        if (!hasText(properties.apiHash())) {
            throw new TelegramClientConfigurationException("Application identifier hash for Telegram API access is invalid. " +
                    "Specify property spring.telegram.client.api-hash" );
        }
        if (!hasText(properties.databaseEncryptionKey())) {
            throw new TelegramClientConfigurationException("Encryption key for the database is invalid. " +
                    "Specify property spring.telegram.client.database-encryption-key" );
        }
        if (!hasText(properties.systemLanguageCode())) {
            throw new TelegramClientConfigurationException("IETF language tag of the user's operating system language; must be non-empty. " +
                    "Specify property spring.telegram.client.system-language-code" );
        }
        if (!hasText(properties.deviceModel())) {
            throw new TelegramClientConfigurationException("Model of the device the application is being run on; must be non-empty. " +
                    "Specify property spring.telegram.client.device-model" );
        }
        TelegramProperties.Proxy proxy = properties.proxy();
        if (proxy != null) {
            checkProxyProperties(proxy);
        }
    }

    private static void checkProxyProperties(TelegramProperties.Proxy proxy) {
        if (!hasText(proxy.server()) || proxy.port() <= 0) {
            throw new TelegramClientConfigurationException("""
                    Proxy settings not filled. Specify properties:
                     spring.telegram.client.proxy.server
                     spring.telegram.client.proxy.port
                    """);
        }
        TelegramProperties.Proxy.ProxyHttp http = proxy.http();
        TelegramProperties.Proxy.ProxySocks5 socks5 = proxy.socks5();
        TelegramProperties.Proxy.ProxyMtProto mtProto = proxy.mtproto();
        if (http != null) {
            if (!hasText(http.password()) || !hasText(http.username())) {
                throw new TelegramClientConfigurationException("""
                        Http proxy settings not filled. Specify properties:
                         spring.telegram.client.proxy.http.username
                         spring.telegram.client.proxy.http.password
                         spring.telegram.client.proxy.http.http-only
                         """);
            }
        } else if (socks5 != null) {
            if (!hasText(socks5.username()) || !hasText(socks5.password())) {
                throw new TelegramClientConfigurationException("""
                        Socks5 proxy settings not filled. Specify properties:
                         spring.telegram.client.proxy.socks5.username
                         spring.telegram.client.proxy.socks5.password
                         """);
            }
        } else if (mtProto != null) {
            if (!hasText(mtProto.secret())) {
                throw new TelegramClientConfigurationException("MtProto proxy settings not filled. " +
                        "Specify property spring.telegram.client.proxy.mtProto.secret" );
            }
        } else {
            throw new TelegramClientConfigurationException("ProxyType not filled. Available types - http, socks5, mtproto" );
        }
    }

    private Client initializeNativeClient(TelegramProperties properties, Collection<UpdateNotificationListener<?>> notificationHandlers) {
        Client.execute(new TdApi.SetLogVerbosityLevel(properties.logVerbosityLevel()));
        Client.LogMessageHandler logMessageHandler = (level, message) -> {
            switch (level) {
                case 0, 1 -> log.error(message);
                case 2 -> log.warn(message);
                case 3 -> log.info(message);
                default -> log.debug(message);
            }
        };
        Client.setLogMessageHandler(properties.logVerbosityLevel(), logMessageHandler);

        return Client.create(new CoreHandler(notificationHandlers), null, null);
    }

    /**
     * {@link TelegramClient} shutdown hook.
     * Properly closing the client.
     */
    @PreDestroy
    void cleanUp() throws InterruptedException {
        sendSync(new TdApi.Close());
        Instant startAwait = Instant.now();
        while (!clientAuthorizationState.isStateClosed() && startAwait.plusSeconds(30).isAfter(Instant.now())) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        if (!clientAuthorizationState.isStateClosed()) {
            log.warn("Closed, but TDLib client isn't in its final state" );
        }
        log.info("Goodbye!" );
    }

    /**
     * Sends a request to the TDLib.
     *
     * @param query object representing a query to the TDLib.
     * @param type  response type
     * @param <T>   parametrized response
     * @return parametrized response from TDLib.
     * @throws TelegramClientTdApiException for request timeout or error response from TDLib.
     * @throws NullPointerException         if query is null.
     */
    public <T extends TdApi.Object> T sendSync(TdApi.Function<? extends TdApi.Object> query,
                                               Class<T> type) {
        TdApi.Object obj = sendSync(query);
        if (obj instanceof TdApi.Error err) {
            throw new TelegramClientTdApiException("Received error from TDLib. ", err);
        }
        return type.cast(obj);
    }

    /**
     * Sends a request to the TDLib.
     *
     * @param query object representing a query to the TDLib.
     * @return response from TDLib.
     * @throws NullPointerException         if query is null.
     * @throws TelegramClientTdApiException for TDLib request timeout.
     */
    public TdApi.Object sendSync(TdApi.Function<? extends TdApi.Object> query) {
        Objects.requireNonNull(query);
        var ref = new AtomicReference<TdApi.Object>();
        client.send(query, ref::set);
        var sent = Instant.now();
        while (ref.get() == null &&
                sent.plus(60, ChronoUnit.SECONDS).isAfter(Instant.now())) {
            /*wait for result*/
        }

        if (ref.get() == null) {
            throw new TelegramClientTdApiException("TDLib request timeout." );
        }

        return ref.get();
    }

    /**
     * Sends a request to the TDLib asynchronously.
     * If this stage completes exceptionally you can handle {@link TelegramClientTdApiException}
     *
     * @param query object representing a query to the TDLib.
     * @return {@link CompletableFuture<TdApi.Object>} response from TDLib.
     * @throws NullPointerException if query is null.
     */
    public CompletableFuture<TdApi.Object> sendAsync(TdApi.Function<? extends TdApi.Object> query) {
        Objects.requireNonNull(query);
        return CompletableFuture.supplyAsync(() -> sendSync(query));
    }

    /**
     * Sends a request to the TDLib asynchronously.
     * If this stage completes exceptionally you can handle {@link TelegramClientTdApiException}
     *
     * @param query object representing a query to the TDLib.
     * @param type  response type
     * @param <T>   parametrized response
     * @return {@link CompletableFuture<T>} parametrized response from TDLib.
     * @throws NullPointerException if query is null.
     */
    public <T extends TdApi.Object> CompletableFuture<T> sendAsync(TdApi.Function<? extends TdApi.Object> query,
                                                                   Class<T> type) {
        Objects.requireNonNull(query);
        return CompletableFuture.supplyAsync(() -> sendSync(query, type));
    }

    /**
     * Sends a request to the TDLib with callback.
     *
     * @param query         object representing a query to the TDLib.
     * @param resultHandler Result handler with onResult method which will be called with result
     *                      of the query or with TdApi.Error as parameter.
     * @throws NullPointerException if query is null.
     */
    public void sendWithCallback(TdApi.Function<? extends TdApi.Object> query,
                                 Client.ResultHandler resultHandler) {
        Objects.requireNonNull(query);
        client.send(query, resultHandler);
    }

    /**
     * The main handler for incoming updates from TDLib.
     */
    private final class CoreHandler implements Client.ResultHandler {

        private final Map<Integer, Consumer<TdApi.Object>> tdUpdateHandlers = new HashMap<>();

        private CoreHandler(Collection<UpdateNotificationListener<?>> notifications) {
            notifications.forEach(ntf -> {
                var handler = new UpdateNotificationConsumer(ntf, ntf.notificationType());
                tdUpdateHandlers.putIfAbsent(getConstructorNumberOfType(ntf), handler);
            });
        }

        private int getConstructorNumberOfType(UpdateNotificationListener<?> updateNotification) {
            try {
                TdApi.Update tmp = updateNotification.notificationType().getConstructor().newInstance();
                return tmp.getConstructor();
            } catch (ReflectiveOperationException e) {
                throw new TelegramClientTdApiException(e.getMessage());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onResult(TdApi.Object object) {
            tdUpdateHandlers.getOrDefault(object.getConstructor(), defaultHandler::onResult).
                    accept(object);
        }

    }
}
