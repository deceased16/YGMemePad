package ru.deceased16.ygmemepad.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class BindingsTransfer {

    private static final String PREFIX = "ygmp1:";

    private BindingsTransfer() {
    }

    public static String export(Map<String, Integer> binds) {
        JsonObject list = new JsonObject();
        new TreeMap<>(binds).forEach(list::addProperty);

        JsonObject root = new JsonObject();
        root.add("binds", list);

        byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static Map<String, Integer> parse(String text) {
        if (text == null) {
            return null;
        }
        String s = text.replaceAll("\\s+", "");
        if (!s.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return null;
        }
        try {
            byte[] raw = Base64.getUrlDecoder().decode(s.substring(PREFIX.length()));
            JsonObject root = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8)).getAsJsonObject();

            Map<String, Integer> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("binds").entrySet()) {
                JsonElement value = entry.getValue();
                if (entry.getKey().isBlank() || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                    continue;
                }
                int code = value.getAsInt();
                if (code >= 0) {
                    result.put(entry.getKey(), code);
                }
            }
            return result.isEmpty() ? null : result;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
