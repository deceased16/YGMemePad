package ru.deceased16.ygmemepad.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MemePadConfig {

    private static final Logger LOGGER = Logger.getLogger("YGMemePad");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("ygmemepad.json");

    private final Map<String, Integer> soundKeys = new HashMap<>();

    private int openMenuKey = GLFW.GLFW_KEY_APOSTROPHE;

    private static MemePadConfig instance;

    private MemePadConfig() {
    }

    public static MemePadConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public Integer getKeyForSound(String soundName) {
        return soundKeys.get(soundName);
    }

    public Map<String, Integer> getBindingsSnapshot() {
        return new HashMap<>(soundKeys);
    }

    public void setKeyForSound(String soundName, int keyCode) {
        soundKeys.entrySet().removeIf(e -> !e.getKey().equals(soundName) && e.getValue() == keyCode);
        soundKeys.put(soundName, keyCode);
        save();
    }

    public void clearKeyForSound(String soundName) {
        soundKeys.remove(soundName);
        save();
    }

    public int getOpenMenuKey() {
        return openMenuKey;
    }

    public void setOpenMenuKey(int keyCode) {
        soundKeys.entrySet().removeIf(e -> e.getValue() == keyCode);
        this.openMenuKey = keyCode;
        save();
    }

    private static MemePadConfig load() {
        MemePadConfig config = new MemePadConfig();
        if (!Files.exists(FILE)) {
            return config;
        }

        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null) {
                if (data.soundKeys != null) {
                    config.soundKeys.putAll(data.soundKeys);
                }
                if (data.openMenuKey != null) {
                    config.openMenuKey = data.openMenuKey;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Не удалось прочитать ygmemepad.json, будет создан заново", e);
        }

        return config;
    }

    private void save() {
        Data data = new Data();
        data.soundKeys.putAll(soundKeys);
        data.openMenuKey = openMenuKey;

        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Не удалось сохранить ygmemepad.json", e);
        }
    }

    private static final class Data {
        Map<String, Integer> soundKeys = new HashMap<>();
        Integer openMenuKey;
    }
}
