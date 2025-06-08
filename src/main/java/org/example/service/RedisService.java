package org.example.service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisService {
    private final JedisPool pool;
    private static final int TTL_SECONDS = 3600; // время жизни ключа – 1 час

    public RedisService() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        this.pool = new JedisPool(host, port);
    }
    //проверяем полученный текст, если такой есть уже в редисе возвращаем тру,

    public boolean checkExist(String text) {
        try (Jedis jedis = pool.getResource()) {
            String textKey = key(text);
            // Проверка наличия текста в Redis
            return jedis.exists(textKey);
        }
    }
    // если такоего нет, но мы определили
    // что это спам - вносим в редис
    public void addInReddis(Long userId, String text){
        try (Jedis jedis = pool.getResource()) {
            String textKey = key(text);
                // Сохраняем ключ с TTL
            jedis.setex(textKey, TTL_SECONDS, userId.toString());
        }
    }

    private String key(Object value) {
        return "user:" + value.toString();
    }
}
