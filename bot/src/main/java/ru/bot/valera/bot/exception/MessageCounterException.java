package ru.bot.valera.bot.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.bot.valera.bot.to.UpdateTO;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageCounterException extends RuntimeException {

    final UpdateTO updateTO;
    final String errorMessage;
}
