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
import java.util.EnumMap;
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

    public enum Layout {
        TILES,
        LIST
    }

    public enum SortMode {
        NAME_ASC,
        NAME_DESC,
        MOST_USED,
        RECENT,
        SERVER;

        public SortMode next() {
            SortMode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    public static final int TILE_WIDTH_MIN = 90;
    public static final int TILE_WIDTH_MAX = 200;
    public static final int TILE_WIDTH_DEFAULT = 112;
    public static final int COLUMNS_MAX = 8;

    private final Map<String, Integer> soundKeys = new HashMap<>();
    private final Map<String, Integer> useCount = new HashMap<>();
    private final Map<String, Long> useLast = new HashMap<>();
    private final Map<MemePadTheme.Slot, Integer> colors = new EnumMap<>(MemePadTheme.Slot.class);
    private Layout layout = Layout.TILES;
    private SortMode sortMode = SortMode.NAME_ASC;
    private int columns = 0;
    private int tileWidth = TILE_WIDTH_DEFAULT;

    private int openMenuKey = GLFW.GLFW_KEY_APOSTROPHE;
    private boolean checkUpdates = true;

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

    public int importBindings(Map<String, Integer> binds) {
        for (Map.Entry<String, Integer> entry : binds.entrySet()) {
            final String name = entry.getKey();
            final int key = entry.getValue();
            soundKeys.entrySet().removeIf(e -> !e.getKey().equals(name) && e.getValue() == key);
            soundKeys.put(name, key);
        }
        int applied = 0;
        for (Map.Entry<String, Integer> entry : binds.entrySet()) {
            Integer current = soundKeys.get(entry.getKey());
            if (current != null && current.equals(entry.getValue())) {
                applied++;
            }
        }
        save();
        return applied;
    }

    public void clearAllBindings() {
        soundKeys.clear();
        save();
    }

    public int getBindingsCount() {
        return soundKeys.size();
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
        save();
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns, boolean persist) {
        this.columns = Math.max(0, Math.min(COLUMNS_MAX, columns));
        if (persist) {
            save();
        }
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth, boolean persist) {
        this.tileWidth = Math.max(TILE_WIDTH_MIN, Math.min(TILE_WIDTH_MAX, tileWidth));
        if (persist) {
            save();
        }
    }

    public void recordUse(String soundName) {
        useCount.merge(soundName, 1, Integer::sum);
        useLast.put(soundName, System.currentTimeMillis());
    }

    public int getUseCount(String soundName) {
        return useCount.getOrDefault(soundName, 0);
    }

    public long getLastUsed(String soundName) {
        return useLast.getOrDefault(soundName, 0L);
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
        save();
    }

    public int getColorRgb(MemePadTheme.Slot slot) {
        Integer value = colors.get(slot);
        if (value != null) {
            return value;
        }
        return MemePadTheme.parseHex(slot.defaultHex());
    }

    public String getColorHex(MemePadTheme.Slot slot) {
        return MemePadTheme.toHex(getColorRgb(slot));
    }

    public int getColorArgb(MemePadTheme.Slot slot) {
        return MemePadTheme.withAlpha(getColorRgb(slot), slot.alpha());
    }

    public int getColorHoverArgb(MemePadTheme.Slot slot) {
        return MemePadTheme.hover(getColorArgb(slot));
    }

    public void setColorRgb(MemePadTheme.Slot slot, int rgb) {
        setColorRgb(slot, rgb, true);
    }

    public void setColorRgb(MemePadTheme.Slot slot, int rgb, boolean persist) {
        colors.put(slot, rgb & 0xFFFFFF);
        if (persist) {
            save();
        }
    }

    public void flush() {
        save();
    }

    public void resetColor(MemePadTheme.Slot slot) {
        colors.remove(slot);
        save();
    }

    public boolean isCheckUpdates() {
        return checkUpdates;
    }

    public void setCheckUpdates(boolean checkUpdates) {
        this.checkUpdates = checkUpdates;
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
                if (data.checkUpdates != null) {
                    config.checkUpdates = data.checkUpdates;
                }
                if (data.layout != null) {
                    try {
                        config.layout = Layout.valueOf(data.layout);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (data.sortMode != null) {
                    try {
                        config.sortMode = SortMode.valueOf(data.sortMode);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (data.columns != null) {
                    config.setColumns(data.columns, false);
                }
                if (data.tileWidth != null) {
                    config.setTileWidth(data.tileWidth, false);
                }
                if (data.colors != null) {
                    for (MemePadTheme.Slot slot : MemePadTheme.Slot.values()) {
                        Integer rgb = MemePadTheme.parseHex(data.colors.get(slot.name()));
                        if (rgb != null) {
                            config.colors.put(slot, rgb);
                        }
                    }
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
        data.checkUpdates = checkUpdates;
        data.layout = layout.name();
        data.sortMode = sortMode.name();
        data.columns = columns;
        data.tileWidth = tileWidth;
        for (Map.Entry<MemePadTheme.Slot, Integer> entry : colors.entrySet()) {
            data.colors.put(entry.getKey().name(), MemePadTheme.toHex(entry.getValue()));
        }

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
        Boolean checkUpdates;
        String layout;
        String sortMode;
        Integer columns;
        Integer tileWidth;
        Map<String, String> colors = new HashMap<>();
    }
}
