package ru.deceased16.ygmemepad.update;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class UpdateHelper {

    private UpdateHelper() {
    }

    public static void main(String[] args) {
        try {
            long pid = Long.parseLong(args[0]);
            Path staged = Path.of(args[1]);
            Path oldJar = Path.of(args[2]);
            Path newJar = Path.of(args[3]);

            ProcessHandle.of(pid).ifPresent(handle -> {
                try {
                    handle.onExit().get(10, TimeUnit.MINUTES);
                } catch (Exception ignored) {
                }
            });

            for (int attempt = 1; attempt <= 120; attempt++) {
                try {
                    if (JarSwap.swap(staged, oldJar, newJar)) {
                        System.out.println("OK: " + oldJar + " -> " + newJar);
                        return;
                    }
                } catch (IOException e) {
                    System.out.println("attempt " + attempt + ": " + e);
                }
                Thread.sleep(500);
            }
            System.out.println("Не удалось заменить файл");
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }
    }
}
