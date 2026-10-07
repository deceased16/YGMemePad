package ru.deceased16.ygmemepad.input;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import ru.deceased16.ygmemepad.YGMemePadClient;
import ru.deceased16.ygmemepad.config.MemePadConfig;
import ru.deceased16.ygmemepad.gui.MemePadScreen;
import ru.deceased16.ygmemepad.network.MemePadActions;
import ru.deceased16.ygmemepad.network.MemeSoundListData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MemePadInput {

    private static final Set<Integer> pressedLastTick = new HashSet<>();
    private static final Map<String, Long> lastTriggeredAtMillis = new HashMap<>();

    private MemePadInput() {
    }

    public static void onTick(MinecraftClient client) {
        if (client.player == null || client.getWindow() == null) {
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        long window = client.getWindow().getHandle();
        MemePadConfig config = MemePadConfig.get();

        while (MemePadKeys.OPEN_MENU.wasPressed()) {
            client.setScreen(new MemePadScreen(null));
        }

        for (Map.Entry<String, Integer> entry : config.getBindingsSnapshot().entrySet()) {
            String soundName = entry.getKey();
            int keyCode = entry.getValue();
            checkKey(window, keyCode, () -> triggerSound(client, soundName));
        }
    }

    private static void checkKey(long window, int keyCode, Runnable onJustPressed) {
        if (keyCode < 0) {
            return;
        }

        boolean pressedNow = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
        boolean pressedBefore = pressedLastTick.contains(keyCode);

        if (pressedNow && !pressedBefore) {
            onJustPressed.run();
        }

        if (pressedNow) {
            pressedLastTick.add(keyCode);
        } else {
            pressedLastTick.remove(keyCode);
        }
    }

    private static void triggerSound(MinecraftClient client, String soundName) {
        if (client.player == null) {
            return;
        }

        MemeSoundListData data = YGMemePadClient.getLastSoundData();
        long cooldownMillis = Math.max(0, data.cooldownSeconds()) * 1000L;
        long now = System.currentTimeMillis();

        Long lastTriggered = lastTriggeredAtMillis.get(soundName);
        if (lastTriggered != null && now - lastTriggered < cooldownMillis) {
            return;
        }

        lastTriggeredAtMillis.put(soundName, now);
        MemePadActions.play(client, soundName);
    }
}
