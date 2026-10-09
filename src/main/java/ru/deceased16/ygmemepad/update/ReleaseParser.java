package ru.deceased16.ygmemepad.update;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReleaseParser {

    private ReleaseParser() {
    }

    public static ReleaseInfo parse(String json) throws IOException {
        return parse(JsonUtil.parseObject(json));
    }

    static ReleaseInfo parse(Map<String, Object> release) throws IOException {
        if (Boolean.TRUE.equals(release.get("draft")) || Boolean.TRUE.equals(release.get("prerelease"))) {
            throw new IOException("Релиз является черновиком или пре-релизом");
        }

        String tag = text(release.get("tag_name"));
        if (tag == null || tag.isBlank()) {
            throw new IOException("В ответе нет tag_name");
        }
        String page = text(release.get("html_url"));

        if (release.get("assets") instanceof List<?> assets) {
            for (Object item : assets) {
                if (!(item instanceof Map<?, ?> asset)) {
                    continue;
                }
                String name = text(asset.get("name"));
                String url = text(asset.get("browser_download_url"));
                if (name == null || url == null || !isModJar(name)) {
                    continue;
                }

                long size = asset.get("size") instanceof Number n ? n.longValue() : -1;
                String digest = text(asset.get("digest"));
                String sha256 = digest != null && digest.toLowerCase(Locale.ROOT).startsWith("sha256:")
                        ? digest.substring("sha256:".length()) : null;

                return new ReleaseInfo(tag, name, url, size, sha256, page);
            }
        }
        throw new IOException("В релизе " + tag + " нет подходящего jar-файла");
    }

    static boolean isModJar(String fileName) {
        String n = fileName.toLowerCase(Locale.ROOT);
        return n.endsWith(".jar") && !n.contains("sources") && !n.contains("javadoc") && !n.contains("-dev");
    }

    private static String text(Object value) {
        return value instanceof String s ? s : null;
    }
}
