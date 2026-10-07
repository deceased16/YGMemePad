package ru.deceased16.ygmemepad.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import ru.deceased16.ygmemepad.config.MemePadConfig;

public final class MemePadKeys {

    public static KeyBinding OPEN_MENU;

    private MemePadKeys() {
    }

    public static void register() {
        int defaultKey = MemePadConfig.get().getOpenMenuKey();

        OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ygmemepad.open_menu",
                InputUtil.Type.KEYSYM,
                defaultKey,
                KeyBinding.Category.create(Identifier.of("ygmemepad", "main"))
        ));
    }
}
