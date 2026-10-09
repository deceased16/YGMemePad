package ru.deceased16.ygmemepad.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import ru.deceased16.ygmemepad.update.UpdateManager;

import java.util.function.Consumer;

public class UpdatePromptScreen extends BaseOwoScreen<FlowLayout> {

    private static final int C_NORMAL = 0xB0202630;
    private static final int C_NORMAL_HOVER = 0xC03A4658;
    private static final int C_OFF = 0x60101418;
    private static final int C_ACCENT = 0xD03A78C2;
    private static final int C_ACCENT_HOVER = 0xE04A8AD6;
    private static final int DIM_COLOR = 0x60000000;
    private final Screen parent;
    private FlowLayout panel;
    private int lastSeenCounter = -1;

    public UpdatePromptScreen(Screen parent) {
        super(Text.translatable("ygmemepad.update.dialog.title"));
        this.parent = parent;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.flat(DIM_COLOR));
        root.horizontalAlignment(HorizontalAlignment.CENTER);
        root.verticalAlignment(VerticalAlignment.CENTER);

        panel = UIContainers.verticalFlow(Sizing.fixed(340), Sizing.content());
        panel.padding(Insets.of(14));
        panel.gap(8);
        panel.surface(Surface.flat(0xF0181C24).and(Surface.outline(0x66FFFFFF)));
        root.child(panel);
    }

    @Override
    protected void init() {
        super.init();
        if (parent != null && this.client != null
                && (parent.width != this.width || parent.height != this.height)) {
            parent.resize(this.width, this.height);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (parent != null) {
            parent.render(context, -1, -1, delta);
        }

        if (UpdateManager.getChangeCounter() != lastSeenCounter) {
            lastSeenCounter = UpdateManager.getChangeCounter();
            rebuild();
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void rebuild() {
        panel.clearChildren();

        String latest = UpdateManager.getLatestVersion();
        String current = UpdateManager.getCurrentVersion();

        switch (UpdateManager.getState()) {
            case AVAILABLE -> {
                title(Text.translatable("ygmemepad.update.dialog.title"));
                body(Text.translatable("ygmemepad.update.dialog.question", latest, current));
                buttons(
                        button(Text.translatable("ygmemepad.update.dialog.accept"), C_ACCENT, C_ACCENT_HOVER,
                                b -> UpdateManager.install()),
                        button(Text.translatable("ygmemepad.update.dialog.decline"), C_NORMAL, C_NORMAL_HOVER,
                                b -> close()));
            }
            case DOWNLOADING -> {
                title(Text.translatable("ygmemepad.update.dialog.downloading_title"));
                body(Text.translatable("ygmemepad.update.downloading", UpdateManager.getPercent() + "%"));
            }
            case READY -> {
                title(Text.translatable("ygmemepad.update.dialog.ready_title"));
                body(Text.translatable("ygmemepad.update.dialog.ready", latest));
                buttons(button(Text.translatable("ygmemepad.update.dialog.ok"), C_ACCENT, C_ACCENT_HOVER,
                        b -> close()));
            }
            case FAILED -> {
                title(Text.translatable("ygmemepad.update.dialog.failed_title"));
                body(Text.translatable("ygmemepad.update.failed", UpdateManager.getError()));
                buttons(
                        button(Text.translatable("ygmemepad.update.retry"), C_ACCENT, C_ACCENT_HOVER,
                                b -> UpdateManager.install()),
                        button(Text.translatable("ygmemepad.update.dialog.close"), C_NORMAL, C_NORMAL_HOVER,
                                b -> close()));
            }
            default -> {
                title(Text.translatable("ygmemepad.update.dialog.title"));
                buttons(button(Text.translatable("ygmemepad.update.dialog.close"), C_NORMAL, C_NORMAL_HOVER,
                        b -> close()));
            }
        }
    }

    private void title(Text text) {
        LabelComponent label = UIComponents.label(text);
        label.shadow(true);
        label.color(Color.ofArgb(0xFFFFD54F));
        panel.child(label);
    }

    private void body(Text text) {
        LabelComponent label = UIComponents.label(text);
        label.horizontalSizing(Sizing.fill());
        panel.child(label);
    }

    private void buttons(ButtonComponent... buttons) {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.gap(8);
        row.horizontalAlignment(HorizontalAlignment.CENTER);
        for (ButtonComponent button : buttons) {
            row.child(button);
        }
        panel.child(row);
    }

    private static ButtonComponent button(Text text, int color, int hover, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = UIComponents.button(text, onPress);
        button.renderer(ButtonComponent.Renderer.flat(color, hover, C_OFF));
        button.sizing(Sizing.fixed(140), Sizing.fixed(20));
        return button;
    }

    @Override
    public void close() {
        if (UpdateManager.getState() == UpdateManager.State.AVAILABLE) {
            UpdateManager.markDeclined();
        }
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
