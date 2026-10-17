package ru.deceased16.ygmemepad.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import ru.deceased16.ygmemepad.config.MemePadConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class KeyConflicts {

    private KeyConflicts() {
    }

    public static List<String> find(MinecraftClient client, String soundName, int keyCode) {
        List<String> result = new ArrayList<>();
        if (keyCode < 0) {
            return result;
        }

        for (Map.Entry<String, Integer> entry : MemePadConfig.get().getBindingsSnapshot().entrySet()) {
            if (!entry.getKey().equals(soundName) && entry.getValue() == keyCode) {
                result.add(entry.getKey());
            }
        }

        if (client != null && client.options != null) {
            String translationKey = InputUtil.Type.KEYSYM.createFromCode(keyCode).getTranslationKey();
            for (KeyBinding binding : client.options.allKeys) {
                if (!binding.isUnbound() && translationKey.equals(binding.getBoundKeyTranslationKey())) {
                    result.add(Text.translatable(binding.getId()).getString());
                }
            }
        }
        return result;
    }
}
