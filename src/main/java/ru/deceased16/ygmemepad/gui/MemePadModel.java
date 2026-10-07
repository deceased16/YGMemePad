package ru.deceased16.ygmemepad.gui;

import ru.deceased16.ygmemepad.network.MemeSoundEntry;
import ru.deceased16.ygmemepad.network.MemeSoundListData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MemePadModel {

    public static final String TAB_ALL = "\0all";
    public static final String TAB_UNCATEGORIZED = "\0none";

    public record Tab(String id, int count) {
        public boolean isAll() {
            return TAB_ALL.equals(id);
        }

        public boolean isUncategorized() {
            return TAB_UNCATEGORIZED.equals(id);
        }
    }

    private MemePadModel() {
    }

    public static List<Tab> tabs(MemeSoundListData data) {
        List<MemeSoundEntry> sounds = data.sounds();

        int uncategorized = 0;
        for (MemeSoundEntry sound : sounds) {
            if (!sound.hasCategory()) {
                uncategorized++;
            }
        }

        List<Tab> tabs = new ArrayList<>();
        tabs.add(new Tab(TAB_ALL, sounds.size()));
        if (uncategorized > 0) {
            tabs.add(new Tab(TAB_UNCATEGORIZED, uncategorized));
        }
        for (String category : data.categories()) {
            tabs.add(new Tab(category, countIn(sounds, category)));
        }
        return tabs;
    }

    public static List<MemeSoundEntry> filter(MemeSoundListData data, String tabId, String query, boolean searchAllTabs) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean ignoreTab = searchAllTabs && !needle.isEmpty();

        List<MemeSoundEntry> result = new ArrayList<>();
        for (MemeSoundEntry sound : data.sounds()) {
            if (!ignoreTab && !inTab(sound, tabId)) {
                continue;
            }
            if (!needle.isEmpty() && !sound.name().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            result.add(sound);
        }
        result.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return result;
    }

    public static String validTab(MemeSoundListData data, String tabId) {
        for (Tab tab : tabs(data)) {
            if (tab.id().equals(tabId)) {
                return tabId;
            }
        }
        return TAB_ALL;
    }

    private static boolean inTab(MemeSoundEntry sound, String tabId) {
        if (TAB_ALL.equals(tabId)) {
            return true;
        }
        if (TAB_UNCATEGORIZED.equals(tabId)) {
            return !sound.hasCategory();
        }
        return tabId.equals(sound.category());
    }

    private static int countIn(List<MemeSoundEntry> sounds, String category) {
        int count = 0;
        for (MemeSoundEntry sound : sounds) {
            if (category.equals(sound.category())) {
                count++;
            }
        }
        return count;
    }
}
