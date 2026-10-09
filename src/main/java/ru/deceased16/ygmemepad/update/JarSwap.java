package ru.deceased16.ygmemepad.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.File;

public final class JarSwap {

    private JarSwap() {
    }

    public static boolean swap(Path staged, Path oldJar, Path newJar) throws IOException {
        Path temp = newJar.resolveSibling(newJar.getFileName() + ".new");
        Files.copy(staged, temp, StandardCopyOption.REPLACE_EXISTING);

        try {
            Files.delete(oldJar);
        } catch (NoSuchFileException e) {
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            return false;
        }

        Files.move(temp, newJar, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(staged);
        return true;
    }

    public static void spawnHelper(Path staged, Path oldJar, Path newJar, Path workDir) throws IOException {
        Files.createDirectories(workDir);
        Path helperJar = workDir.resolve("helper.jar");
        Files.copy(oldJar, helperJar, StandardCopyOption.REPLACE_EXISTING);

        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        Path java = bin.resolve(windows ? "javaw.exe" : "java");
        if (!Files.exists(java)) {
            java = bin.resolve(windows ? "java.exe" : "java");
        }

        ProcessBuilder builder = new ProcessBuilder(
                java.toString(), "-cp", helperJar.toString(), UpdateHelper.class.getName(),
                Long.toString(ProcessHandle.current().pid()),
                staged.toString(), oldJar.toString(), newJar.toString());
        builder.redirectErrorStream(true);
        File log = workDir.resolve("helper.log").toFile();
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        builder.start();
    }
}
