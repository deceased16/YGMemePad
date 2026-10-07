package ru.deceased16.ygmemepad.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.deceased16.ygmemepad.network.MemePadActions;

import java.util.function.Supplier;

public class AddCategoryScreen extends Screen {

    private final Supplier<Screen> returnTo;
    private TextFieldWidget nameField;
    private String name = "";

    public AddCategoryScreen(Supplier<Screen> returnTo) {
        super(Text.translatable("ygmemepad.admin.add_category"));
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = this.height / 2 - 30;

        nameField = new TextFieldWidget(this.textRenderer, cx - 100, y + 12, 200, 20,
                Text.translatable("ygmemepad.admin.category_name"));
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.translatable("ygmemepad.admin.category_name"));
        nameField.setText(name);
        nameField.setChangedListener(text -> name = text);
        addDrawableChild(nameField);
        setInitialFocus(nameField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("ygmemepad.admin.create"), b -> submit())
                .dimensions(cx - 100, y + 44, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
                .dimensions(cx + 2, y + 44, 98, 20).build());
    }

    private void submit() {
        String value = name.trim();
        if (value.isEmpty()) {
            return;
        }
        MemePadActions.createCategory(value);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFFFF);
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
