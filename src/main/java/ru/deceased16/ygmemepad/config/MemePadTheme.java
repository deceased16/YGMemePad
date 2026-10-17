package ru.deceased16.ygmemepad.config;

import java.util.Locale;

public final class MemePadTheme {

    public enum Slot {
        BUTTON("202630", 0xB0),
        KEY("141A2A", 0xB0),
        ACCENT("3A78C2", 0xD0);

        private final String defaultHex;
        private final int alpha;

        Slot(String defaultHex, int alpha) {
            this.defaultHex = defaultHex;
            this.alpha = alpha;
        }

        public String defaultHex() {
            return defaultHex;
        }

        public int alpha() {
            return alpha;
        }
    }

    private MemePadTheme() {
    }

    public static Integer parseHex(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() == 3) {
            StringBuilder expanded = new StringBuilder(6);
            for (int i = 0; i < 3; i++) {
                expanded.append(s.charAt(i)).append(s.charAt(i));
            }
            s = expanded.toString();
        }
        if (s.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    public static int hover(int argb) {
        int a = Math.min(0xFF, ((argb >>> 24) & 0xFF) + 0x10);
        int r = lighten((argb >> 16) & 0xFF);
        int g = lighten((argb >> 8) & 0xFF);
        int b = lighten(argb & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lighten(int channel) {
        return channel + (255 - channel) * 18 / 100;
    }
}
