package ru.deceased16.ygmemepad.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class UpdateDownloader {

    public interface Progress {
        void onProgress(long done, long total);
    }

    private static final long MAX_SIZE = 64L * 1024 * 1024;

    static boolean allowAnyUrl = false;

    private UpdateDownloader() {
    }

    public static String fetchText(String url, String userAgent) throws IOException {
        HttpURLConnection connection = open(url, userAgent);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        int code = connection.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code);
        }
        try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder out = new StringBuilder();
            char[] buffer = new char[4096];
            int n;
            while ((n = reader.read(buffer)) > 0) {
                out.append(buffer, 0, n);
                if (out.length() > 2_000_000) {
                    throw new IOException("Слишком большой ответ");
                }
            }
            return out.toString();
        }
    }

    public static void download(String url, Path target, long expectedSize, String expectedSha256,
                                String userAgent, Progress progress) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path part = target.resolveSibling(target.getFileName() + ".part");

        try {
            HttpURLConnection connection = open(url, userAgent);
            int code = connection.getResponseCode();
            if (code != 200) {
                throw new IOException("HTTP " + code);
            }

            long total = connection.getContentLengthLong();
            if (total <= 0) {
                total = expectedSize;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(part)) {
                byte[] buffer = new byte[16 * 1024];
                long done = 0;
                int n;
                while ((n = in.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                    digest.update(buffer, 0, n);
                    done += n;
                    if (done > MAX_SIZE) {
                        throw new IOException("Файл слишком большой");
                    }
                    progress.onProgress(done, total);
                }
            }

            if (expectedSize > 0 && Files.size(part) != expectedSize) {
                throw new IOException("Размер файла не совпадает с ожидаемым");
            }
            if (expectedSha256 != null && !hex(digest.digest()).equalsIgnoreCase(expectedSha256.trim())) {
                throw new IOException("Контрольная сумма не совпадает");
            }

            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally {
            Files.deleteIfExists(part);
        }
    }

    public static String readModVersion(Path jar, String expectedId) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            if (entry == null) {
                throw new IOException("В файле нет fabric.mod.json");
            }
            String json;
            try (InputStream in = zip.getInputStream(entry)) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            Map<String, Object> mod = JsonUtil.parseObject(json);
            if (!expectedId.equals(mod.get("id"))) {
                throw new IOException("Это не jar мода " + expectedId);
            }
            if (!(mod.get("version") instanceof String version)) {
                throw new IOException("В fabric.mod.json нет версии");
            }
            return version;
        }
    }

    private static HttpURLConnection open(String url, String userAgent) throws IOException {
        URI uri = URI.create(url);
        if (!allowAnyUrl && !isAllowed(uri)) {
            throw new IOException("Адрес не разрешён: " + uri.getHost());
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", userAgent);
        return connection;
    }

    static boolean isAllowed(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return "https".equalsIgnoreCase(uri.getScheme())
                && (host.equals("github.com") || host.endsWith(".github.com") || host.endsWith(".githubusercontent.com"));
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
