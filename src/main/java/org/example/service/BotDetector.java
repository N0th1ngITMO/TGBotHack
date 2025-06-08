package org.example.service;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class BotDetector {
    private static final Pattern COMPILED_PATTERN = Pattern.compile(
            String.join("|", new String[]{
                    "(ищу\\s+парня|ищу\\s+девушку|одинок[а-я]{1})",
                    "(секс|интим|18\\+|обнаженн[а-я]{1})",
                    "(только\\s+сегодня|только\\s+для\\s+тебя)",
                    "(заработай\\s+\\d+\\s*₽)",
                    "(в\\s+профиль|по\\s+ссылке|личку|приват)",
                    "(my\\s+onlyfans|hot\\s+girl|click\\s+here|dm\\s+me)",
                    "(escort|fuck\\s+me|horny|wanna\\s+play|free\\s+nudes)",
                    "(💋|💦|🔥|🍑|👅|❤️|😘)"
            }),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public static boolean isShlyuhobot(String text) {
        if (text == null) return false;
        Matcher matcher = COMPILED_PATTERN.matcher(text);
        return matcher.find();
    }
}
