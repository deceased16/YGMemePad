package ru.deceased16.ygmemepad.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.deceased16.ygmemepad.YGMemePadClient;
import ru.deceased16.ygmemepad.network.MemePadActions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AddSoundScreen extends Screen {

    private final Supplier<Screen> returnTo;

    private String name = "";
    private String soundEvent = "";
    private int categoryIndex = 0;
    private final List<String> categories = new ArrayList<>();

    private TextFieldWidget nameField;
    private TextFieldWidget eventField;
    private ButtonWidget categoryButton;

    public AddSoundScreen(Supplier<Screen> returnTo, String defaultCategory) {
        super(Text.translatable("ygmemepad.admin.add_sound"));
        this.returnTo = returnTo;

        categories.add("");
        categories.addAll(YGMemePadClient.getLastSoundData().categories());
        if (defaultCategory != null) {
            int index = categories.indexOf(defaultCategory);
            if (index >= 0) {
                categoryIndex = index;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = this.height / 2 - 62;

        nameField = new TextFieldWidget(this.textRenderer, cx - 100, y + 24, 200, 20,
                Text.translatable("ygmemepad.admin.sound_name"));
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.translatable("ygmemepad.admin.sound_name"));
        nameField.setText(name);
        nameField.setChangedListener(text -> name = text);
        addDrawableChild(nameField);

        eventField = new TextFieldWidget(this.textRenderer, cx - 100, y + 62, 200, 20,
                Text.translatable("ygmemepad.admin.sound_event"));
        eventField.setMaxLength(128);
        eventField.setPlaceholder(Text.translatable("ygmemepad.admin.sound_event"));
        eventField.setText(soundEvent);
        eventField.setChangedListener(text -> soundEvent = text);
        addDrawableChild(eventField);

        categoryButton = addDrawableChild(ButtonWidget.builder(categoryLabel(), b -> {
                    categoryIndex = (categoryIndex + 1) % categories.size();
                    categoryButton.setMessage(categoryLabel());
                })
                .dimensions(cx - 100, y + 92, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("ygmemepad.admin.create"), b -> submit())
                .dimensions(cx - 100, y + 124, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
                .dimensions(cx + 2, y + 124, 98, 20).build());

        setInitialFocus(nameField);
    }

    private Text categoryLabel() {
        String category = categories.get(categoryIndex);
        Text value = category.isEmpty() ? Text.translatable("ygmemepad.tab.uncategorized") : Text.literal(category);
        return Text.translatable("ygmemepad.admin.category_label", value);
    }

    private void submit() {
        String soundName = name.trim();
        String event = soundEvent.trim();
        if (soundName.isEmpty() || event.isEmpty()) {
            return;
        }
        MemePadActions.createSound(soundName, event, categories.get(categoryIndex));
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        super.render(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int y = this.height / 2 - 62;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, y, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("ygmemepad.admin.sound_name"), cx - 100, y + 14, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("ygmemepad.admin.sound_event"), cx - 100, y + 52, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(returnTo.get());
        }
    }
}
