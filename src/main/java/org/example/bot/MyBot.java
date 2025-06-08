package org.example.bot;

import org.example.dto.MessagePayload;
import org.example.service.DBService;
import org.example.service.LinkService;
import org.example.service.RedisService;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public class MyBot extends TelegramLongPollingBot {
    private Integer spamCount = 1;
    private final LinkService linkService = new LinkService();
    private final DBService dbService = new DBService();
    private final String BOT_USERNAME =
            System.getenv().getOrDefault("BOT_USERNAME", "@gornichnaya_antispam_bot");
    RedisService redisService = new RedisService();
    public MyBot() {
        super("7543300982:AAGV6PNVoE9HWasWz2A6Su9GH_NdGZ10i2U");   // токен только из env!
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message msg  = update.getMessage();
        Long chatId = msg.getChatId();
        Long userId  = msg.getFrom().getId();
        String text = extractText(msg);
        if (text == null || text.isBlank()) {
            return; // ничего не обрабатывать
        }

        // --------ПРОВЕРКИ--------- //
        boolean found = redisService.checkExist(text);
        if (found) {
            try {
                execute(new DeleteMessage(chatId.toString(), msg.getMessageId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            // в бд занести айдишник
        }


//        MessagePayload payload = new MessagePayload(userId, text);
        // рандомный микросервис и таких втыкаем сколько хотим видимо
        // проверка первого микросервиса
        try {
            linkService.sendPayload(text)
                    .thenAccept(isSpam -> {
                        if (isSpam == 1) {
                            System.out.println("Это СПАМ.");
                            redisService.addInReddis(userId, text);
                            try {
                                execute(new DeleteMessage(chatId.toString(), msg.getMessageId()));
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Это НЕ спам.");
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            linkService.sendPayload(text)
                    .thenAccept(random -> {

                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace(); // можно логировать или сохранять ошибку в системе мониторинга
                        return null;
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            Double result = dbService.userId(userId).join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // проверка спам ли, если спам то добавялем в редис
        if (spamCount == 1){redisService.addInReddis(userId, text);}



        // --------ПРОВЕРКИ--------- //
    }

    private String extractText(Message msg) {
        if (msg.hasText()) {
            return msg.getText();
        } else if (msg.getCaption() != null) {
            return msg.getCaption();
        }
        return null;
    }
}
