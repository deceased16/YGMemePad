package ru.deceased16.ygmemepad.update;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.Map;

final class JsonUtil {

    private static final Gson GSON = new Gson();

    private JsonUtil() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(String json) throws IOException {
        try {
            Object value = GSON.fromJson(json, Object.class);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (JsonParseException e) {
            throw new IOException("Некорректный JSON: " + e.getMessage(), e);
        }
        throw new IOException("Ожидался JSON-объект");
    }
}
