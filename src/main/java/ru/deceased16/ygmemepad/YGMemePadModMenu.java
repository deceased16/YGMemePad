package ru.deceased16.ygmemepad;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import ru.deceased16.ygmemepad.gui.MemePadScreen;

public class YGMemePadModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MemePadScreen(parent);
    }
}
