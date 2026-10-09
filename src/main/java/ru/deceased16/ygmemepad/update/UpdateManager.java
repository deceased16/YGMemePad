package ru.deceased16.ygmemepad.update;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.deceased16.ygmemepad.config.MemePadConfig;
import ru.deceased16.ygmemepad.gui.UpdatePromptScreen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class UpdateManager {

    public enum State { IDLE, DISABLED, CHECKING, UP_TO_DATE, CHECK_FAILED, AVAILABLE, DOWNLOADING, READY, FAILED }

    private static final Logger LOGGER = LoggerFactory.getLogger("YGMemePad");
    private static final String MOD_ID = "ygmemepad";
    private static final String API = "https://api.github.com/repos/deceased16/YGMemePad/releases/latest";

    private static volatile State state = State.IDLE;
    private static volatile ReleaseInfo release;
    private static volatile String latestVersion = "";
    private static volatile String error = "";
    private static volatile int percent = 0;

    private static volatile Path stagedJar;
    private static volatile Path newJarPath;

    private static final AtomicInteger changeCounter = new AtomicInteger();
    private static boolean hookRegistered;
    private static boolean chatNotified;
    private static boolean promptShown;

    private UpdateManager() {
    }

    public static State getState() {
        return state;
    }

    public static String getCurrentVersion() {
        return currentVersion();
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static String getError() {
        return error;
    }

    public static int getPercent() {
        return percent;
    }

    public static int getChangeCounter() {
        return changeCounter.get();
    }


    public static void start() {
        if (!MemePadConfig.get().isCheckUpdates()) {
            disable("отключено в ygmemepad.json (checkUpdates)");
            return;
        }
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            disable("режим разработки");
            return;
        }
        Path jar = currentJar();
        if (jar == null) {
            disable("не удалось найти jar-файл мода");
            return;
        }
        LOGGER.info("[YGMemePad] Автообновление: версия {}, файл {}", currentVersion(), jar);

        Path dir = updateDir();
        cleanupHelper(dir);
        if (recoverStaged(dir, jar)) {
            return;
        }
        runAsync(UpdateManager::check);
    }

    public static void checkAgain() {
        if (state == State.UP_TO_DATE || state == State.CHECK_FAILED) {
            runAsync(UpdateManager::check);
        }
    }

    private static void check() {
        setState(State.CHECKING);
        LOGGER.info("[YGMemePad] Проверка обновлений: {}", API);
        try {
            ReleaseInfo info = ReleaseParser.parse(UpdateDownloader.fetchText(API, userAgent()));
            release = info;
            latestVersion = info.version();
            LOGGER.info("[YGMemePad] Последний релиз: {} (файл {}), установлена {}",
                    info.version(), info.assetName(), currentVersion());

            if (Version.compare(info.version(), currentVersion()) > 0) {
                setState(State.AVAILABLE);
                MinecraftClient.getInstance().execute(() -> {
                    promptIfPossible();
                    notifyIfPossible();
                });
            } else {
                setState(State.UP_TO_DATE);
            }
        } catch (Exception e) {
            LOGGER.warn("[YGMemePad] Проверка обновлений не удалась: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            error = describe(e);
            setState(State.CHECK_FAILED);
        }
    }

    private static String describe(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (message.contains("HTTP 404")) {
            return "на GitHub нет опубликованного релиза (HTTP 404)";
        }
        if (e instanceof java.net.SocketTimeoutException || e instanceof java.net.UnknownHostException
                || e instanceof java.net.ConnectException || e instanceof javax.net.ssl.SSLException) {
            return "нет связи с GitHub (" + e.getClass().getSimpleName() + ")";
        }
        return message;
    }

    public static void promptIfPossible() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (promptShown || state != State.AVAILABLE || !(client.currentScreen instanceof TitleScreen)) {
            return;
        }
        promptShown = true;
        client.setScreen(new UpdatePromptScreen(client.currentScreen));
    }

    public static void markDeclined() {
        chatNotified = true;
    }

    public static void notifyIfPossible() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (chatNotified || state != State.AVAILABLE || client.player == null) {
            return;
        }
        chatNotified = true;
        client.player.sendMessage(Text.translatable("ygmemepad.update.chat", latestVersion), false);
    }

    public static void install() {
        ReleaseInfo info = release;
        if ((state != State.AVAILABLE && state != State.FAILED) || info == null) {
            return;
        }
        Path jar = currentJar();
        if (jar == null) {
            fail("мод запущен не из jar-файла");
            return;
        }

        percent = 0;
        setState(State.DOWNLOADING);
        runAsync(() -> {
            Path staged = updateDir().resolve(info.assetName());
            try {
                UpdateDownloader.download(info.downloadUrl(), staged, info.size(), info.sha256(), userAgent(),
                        (done, total) -> {
                            int value = total > 0 ? (int) (done * 100 / total) : 0;
                            if (value != percent) {
                                percent = value;
                                changeCounter.incrementAndGet();
                            }
                        });

                String version = UpdateDownloader.readModVersion(staged, MOD_ID);
                if (Version.compare(version, info.version()) != 0) {
                    throw new IOException("версия внутри файла (" + version + ") не совпадает с релизом");
                }

                stagedJar = staged;
                newJarPath = jar.resolveSibling(info.assetName());
                registerHook();
                setState(State.READY);
            } catch (Exception e) {
                try {
                    Files.deleteIfExists(staged);
                } catch (IOException ignored) {
                }
                LOGGER.warn("[YGMemePad] Не удалось скачать обновление", e);
                fail(e.getMessage());
            }
        });
    }

    private static synchronized void registerHook() {
        if (hookRegistered) {
            return;
        }
        hookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(UpdateManager::applyOnExit, "YGMemePad-ApplyUpdate"));
    }

    private static void applyOnExit() {
        Path staged = stagedJar;
        Path target = newJarPath;
        Path jar = currentJar();
        if (staged == null || target == null || jar == null || !Files.exists(staged)) {
            return;
        }
        try {
            if (!JarSwap.swap(staged, jar, target)) {
                JarSwap.spawnHelper(staged, jar, target, updateDir());
            }
        } catch (Throwable t) {
        }
    }

    private static boolean recoverStaged(Path dir, Path jar) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.getFileName().toString();
                if (name.endsWith(".part")) {
                    Files.deleteIfExists(file);
                    continue;
                }
                if (!name.endsWith(".jar") || name.equals("helper.jar")) {
                    continue;
                }
                try {
                    String version = UpdateDownloader.readModVersion(file, MOD_ID);
                    if (Version.compare(version, currentVersion()) > 0) {
                        stagedJar = file;
                        newJarPath = jar.resolveSibling(name);
                        latestVersion = version;
                        registerHook();
                        setState(State.READY);
                        return true;
                    }
                } catch (IOException ignored) {
                }
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            LOGGER.info("[YGMemePad] Не удалось просмотреть папку обновлений: {}", e.getMessage());
        }
        return false;
    }

    private static void cleanupHelper(Path dir) {
        try {
            Files.deleteIfExists(dir.resolve("helper.jar"));
        } catch (IOException ignored) {
        }
    }

    private static Path currentJar() {
        try {
            Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
            if (container.isEmpty()) {
                return null;
            }
            List<Path> paths = container.get().getOrigin().getPaths();
            if (paths.size() != 1) {
                return null;
            }
            Path path = paths.get(0);
            return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar") ? path : null;
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private static String currentVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("0");
    }

    private static Path updateDir() {
        return FabricLoader.getInstance().getGameDir().resolve("ygmemepad-update");
    }

    private static String userAgent() {
        return "YGMemePad/" + currentVersion();
    }

    private static void disable(String reason) {
        LOGGER.info("[YGMemePad] Автообновление выключено: {}", reason);
        error = reason;
        setState(State.DISABLED);
    }

    private static void fail(String message) {
        error = message == null ? "неизвестная ошибка" : message;
        setState(State.FAILED);
    }

    private static void setState(State newState) {
        state = newState;
        changeCounter.incrementAndGet();
    }

    private static void runAsync(Runnable task) {
        Thread thread = new Thread(task, "YGMemePad-Update");
        thread.setDaemon(true);
        thread.start();
    }
}
