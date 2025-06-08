package org.example.bot;

import org.example.service.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MyBot extends TelegramLongPollingBot {
    private final HuiService huiService = new HuiService();
    private final LinkService linkService = new LinkService();
    private final DBService dbService = new DBService();
    private final AiCheckService aiCheckService = new AiCheckService();
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

        boolean isBot = BotDetector.isShlyuhobot(text);
        if (isBot){redisService.addInReddis(text);
            try {
                execute(new DeleteMessage(chatId.toString(), msg.getMessageId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }}

        try { // NEW
            huiService.sendText(text)
                    .thenAccept(isCursed -> {
                        if (isCursed == 0) {
                            System.out.println("HUI");
                        } else {
                            try {
                                redisService.addInReddis(text);
                                execute(new DeleteMessage(chatId.toString(), msg.getMessageId()));
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        } catch (Exception e) { // NEW
            e.printStackTrace(); // NEW
        }
        if (text.equals("/start")) {
            String info = """
                Привет!
Я AI-горничная для твоего тг канала 🧹

В мой функционал входит:

🧹 Обнаружение и очистка спама с помощью современных ml-решений

👾 Детекция нейросетевого спама с помощью LLM от Google

🛡 Надежная система кеширования для защиты от спам-атак

Чтобы воспользоваться моими функциями, нужно:
 - Добавить меня в телеграм-чат
 - Выдать права администратора

Мы находимся в демонстрационной версии бота, так что в чате ниже ты можешь проверить мой функционал, просто отправь немного спама, и я ловко его почищу 😉
                """;

            SendMessage response = new SendMessage(chatId.toString(), info);
            try {
                execute(response);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        if (redisService.isRateLimited(userId, 5, 10)) { // 5 сообщений за 10 секунд
            try {
                execute(new DeleteMessage(chatId.toString(), msg.getMessageId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
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
        if (text.length() > 5) {
            try {
                linkService.sendPayload(text)
                        .thenAccept(isSpam -> {
                            if (isSpam == 1) {
                                System.out.println("Это СПАМ.");
                                redisService.addInReddis(text);
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

            aiCheckService.sendText(text)
                    .thenAccept(isHuman -> {
                        if (isHuman == 1) {
                            System.out.println("Это человек!");
                        } else {
                            redisService.addInReddis(text);
                            try {
                                execute(new DeleteMessage(chatId.toString(), msg.getMessageId()));
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        }

        try {
            Double result = dbService.userId(userId).join();

        } catch (Exception e) {
            e.printStackTrace();
        }
//
//        // проверка спам ли, если спам то добавялем в редис
//        if (spamCount == 1){redisService.addInReddis(userId, text);}



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
