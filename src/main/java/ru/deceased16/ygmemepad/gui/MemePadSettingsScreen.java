package ru.deceased16.ygmemepad.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
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
import ru.deceased16.ygmemepad.config.BindingsTransfer;
import ru.deceased16.ygmemepad.config.MemePadConfig;
import ru.deceased16.ygmemepad.config.MemePadTheme;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MemePadSettingsScreen extends BaseOwoScreen<FlowLayout> {

    private static final int PANEL_W = 300;
    private static final long CONFIRM_TIMEOUT_MS = 5000;

    private static final int C_DANGER = 0xD0A83232;
    private static final int C_DANGER_HOVER = 0xE0C43E3E;
    private static final int C_OFF = 0x60101418;
    private static final int COLOR_VALID = 0xFFE0E0E0;
    private static final int COLOR_INVALID = 0xFFFF5555;
    private static final int COLOR_OK = 0xFF55FF55;
    private static final int COLOR_WARN = 0xFFFFD54F;

    private final Supplier<Screen> returnTo;

    private ButtonComponent layoutButton;
    private DiscreteSliderComponent columnsSlider;
    private DiscreteSliderComponent tileSlider;
    private ButtonComponent resetBindsButton;
    private LabelComponent statusLabel;
    private boolean resetArmed = false;
    private long resetArmedAt = 0;

    public MemePadSettingsScreen(Supplier<Screen> returnTo) {
        super(Text.translatable("ygmemepad.settings.title"));
        this.returnTo = returnTo;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        MemePadConfig config = MemePadConfig.get();

        root.surface(Surface.flat(0xC0101010));
        root.horizontalAlignment(HorizontalAlignment.CENTER);
        root.verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
        panel.gap(6);
        panel.padding(Insets.of(10));
        panel.surface(Surface.flat(0x90000000).and(Surface.outline(0x28FFFFFF)));

        LabelComponent title = UIComponents.label(this.title);
        title.shadow(true);
        title.horizontalSizing(Sizing.fill());
        title.horizontalTextAlignment(HorizontalAlignment.CENTER);
        panel.child(title);
        panel.child(sectionLabel("ygmemepad.settings.section.layout"));
        layoutButton = button(layoutText(config.getLayout()), PANEL_W - 28, 20,
                config.getColorArgb(MemePadTheme.Slot.BUTTON), config.getColorHoverArgb(MemePadTheme.Slot.BUTTON),
                b -> toggleLayout());
        panel.child(layoutButton);

        columnsSlider = UIComponents.discreteSlider(Sizing.fixed(PANEL_W - 28), 0, MemePadConfig.COLUMNS_MAX);
        columnsSlider.snap(true);
        columnsSlider.scrollStep(1.0 / MemePadConfig.COLUMNS_MAX);
        columnsSlider.message(v -> "0".equals(v)
                ? Text.translatable("ygmemepad.settings.columns_auto")
                : Text.translatable("ygmemepad.settings.columns", v));
        columnsSlider.setFromDiscreteValue(config.getColumns());
        columnsSlider.onChanged().subscribe(v -> {
            config.setColumns((int) Math.round(v), false);
            refreshTileControls();
        });
        columnsSlider.slideEnd().subscribe(config::flush);
        panel.child(columnsSlider);

        tileSlider = UIComponents.discreteSlider(Sizing.fixed(PANEL_W - 28),
                MemePadConfig.TILE_WIDTH_MIN, MemePadConfig.TILE_WIDTH_MAX);
        tileSlider.snap(true);
        tileSlider.scrollStep(4.0 / (MemePadConfig.TILE_WIDTH_MAX - MemePadConfig.TILE_WIDTH_MIN));
        tileSlider.message(v -> Text.translatable("ygmemepad.settings.tile_width", v));
        tileSlider.setFromDiscreteValue(config.getTileWidth());
        tileSlider.onChanged().subscribe(v -> config.setTileWidth((int) Math.round(v), false));
        tileSlider.slideEnd().subscribe(config::flush);
        panel.child(tileSlider);
        refreshTileControls();

        panel.child(sectionLabel("ygmemepad.settings.section.colors"));
        for (MemePadTheme.Slot slot : MemePadTheme.Slot.values()) {
            panel.child(colorSection(slot));
        }

        panel.child(sectionLabel("ygmemepad.settings.section.binds"));
        resetBindsButton = button(Text.translatable("ygmemepad.settings.reset_binds"), PANEL_W - 28, 20,
                config.getColorArgb(MemePadTheme.Slot.BUTTON), config.getColorHoverArgb(MemePadTheme.Slot.BUTTON),
                b -> onResetBindsPressed());
        panel.child(resetBindsButton);

        FlowLayout transfer = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        transfer.gap(4);
        int half = (PANEL_W - 28 - 4) / 2;
        transfer.child(button(Text.translatable("ygmemepad.settings.export"), half, 20,
                config.getColorArgb(MemePadTheme.Slot.BUTTON), config.getColorHoverArgb(MemePadTheme.Slot.BUTTON),
                b -> exportBinds()));
        transfer.child(button(Text.translatable("ygmemepad.settings.import"), half, 20,
                config.getColorArgb(MemePadTheme.Slot.BUTTON), config.getColorHoverArgb(MemePadTheme.Slot.BUTTON),
                b -> importBinds()));
        panel.child(transfer);

        statusLabel = UIComponents.label(Text.empty());
        panel.child(statusLabel);
        FlowLayout buttons = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        buttons.horizontalAlignment(HorizontalAlignment.RIGHT);
        buttons.child(button(Text.translatable("gui.done"), 80, 20,
                config.getColorArgb(MemePadTheme.Slot.BUTTON), config.getColorHoverArgb(MemePadTheme.Slot.BUTTON),
                b -> close()));
        panel.child(buttons);

        ScrollContainer<FlowLayout> scroll = UIContainers.verticalScroll(Sizing.fixed(PANEL_W), Sizing.fill(92), panel);
        scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.WHITE));
        scroll.scrollbarThiccness(4);
        root.child(scroll);
    }

    private LabelComponent sectionLabel(String key) {
        LabelComponent label = UIComponents.label(Text.translatable(key));
        label.color(Color.ofArgb(0xFFAAAAAA));
        label.margins(Insets.top(4));
        return label;
    }

    private CollapsibleContainer colorSection(MemePadTheme.Slot slot) {
        MemePadConfig config = MemePadConfig.get();

        CollapsibleContainer section = UIContainers.collapsible(Sizing.fill(), Sizing.content(),
                Text.translatable("ygmemepad.settings.color." + slot.name().toLowerCase(Locale.ROOT)), false);
        section.gap(4);

        BoxComponent swatch = UIComponents.box(Sizing.fixed(24), Sizing.fixed(10));
        swatch.fill(true);
        swatch.color(Color.ofRgb(config.getColorRgb(slot)));
        swatch.margins(Insets.left(6));
        section.titleLayout().child(swatch);

        ColorPickerComponent picker = new ColorPickerComponent();
        picker.selectorWidth(14);
        picker.selectorPadding(6);
        picker.selectedColor(Color.ofRgb(config.getColorRgb(slot)));
        picker.sizing(Sizing.fixed(PANEL_W - 60), Sizing.fixed(70));
        picker.margins(Insets.of(4, 0, 0, 0));
        section.child(picker);

        FlowLayout inputs = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        inputs.gap(4);
        inputs.verticalAlignment(VerticalAlignment.CENTER);

        TextBoxComponent field = UIComponents.textBox(Sizing.fixed(68));
        field.setMaxLength(7);
        field.setTextPredicate(s -> s.matches("#?[0-9a-fA-F]*"));
        field.text("#" + config.getColorHex(slot));
        inputs.child(field);

        boolean[] syncing = {false};

        picker.onChanged().subscribe(color -> {
            if (syncing[0]) {
                return;
            }
            syncing[0] = true;
            int rgb = color.rgb();
            field.text("#" + MemePadTheme.toHex(rgb));
            field.setEditableColor(COLOR_VALID);
            swatch.color(Color.ofRgb(rgb));
            config.setColorRgb(slot, rgb, false);
            syncing[0] = false;
        });

        field.onChanged().subscribe(text -> {
            if (syncing[0]) {
                return;
            }
            Integer rgb = MemePadTheme.parseHex(text);
            if (rgb == null) {
                field.setEditableColor(COLOR_INVALID);
                return;
            }
            syncing[0] = true;
            field.setEditableColor(COLOR_VALID);
            picker.selectedColor(Color.ofRgb(rgb));
            swatch.color(Color.ofRgb(rgb));
            config.setColorRgb(slot, rgb);
            syncing[0] = false;
        });

        inputs.child(button(Text.translatable("ygmemepad.settings.reset_color"), 50, 20,
                config.getColorArgb(MemePadTheme.Slot.BUTTON), config.getColorHoverArgb(MemePadTheme.Slot.BUTTON),
                b -> {
                    config.resetColor(slot);
                    int rgb = config.getColorRgb(slot);
                    syncing[0] = true;
                    field.text("#" + config.getColorHex(slot));
                    field.setEditableColor(COLOR_VALID);
                    picker.selectedColor(Color.ofRgb(rgb));
                    swatch.color(Color.ofRgb(rgb));
                    syncing[0] = false;
                }));
        section.child(inputs);
        return section;
    }

    private void toggleLayout() {
        MemePadConfig config = MemePadConfig.get();
        MemePadConfig.Layout next = config.getLayout() == MemePadConfig.Layout.TILES
                ? MemePadConfig.Layout.LIST : MemePadConfig.Layout.TILES;
        config.setLayout(next);
        layoutButton.setMessage(layoutText(next));
        refreshTileControls();
    }

    private void refreshTileControls() {
        MemePadConfig config = MemePadConfig.get();
        boolean tiles = config.getLayout() == MemePadConfig.Layout.TILES;
        columnsSlider.active(tiles);
        tileSlider.active(tiles && config.getColumns() == 0);
    }

    private void showStatus(Text text, int argb) {
        statusLabel.text(text);
        statusLabel.color(Color.ofArgb(argb));
    }

    private void exportBinds() {
        Map<String, Integer> binds = MemePadConfig.get().getBindingsSnapshot();
        if (binds.isEmpty()) {
            showStatus(Text.translatable("ygmemepad.settings.export_empty"), COLOR_WARN);
            return;
        }
        if (this.client != null) {
            this.client.keyboard.setClipboard(BindingsTransfer.export(binds));
        }
        showStatus(Text.translatable("ygmemepad.settings.export_done", binds.size()), COLOR_OK);
    }

    private void importBinds() {
        String clipboard = this.client != null ? this.client.keyboard.getClipboard() : null;
        Map<String, Integer> parsed = BindingsTransfer.parse(clipboard);
        if (parsed == null) {
            showStatus(Text.translatable("ygmemepad.settings.import_failed"), COLOR_INVALID);
            return;
        }
        int applied = MemePadConfig.get().importBindings(parsed);
        showStatus(Text.translatable("ygmemepad.settings.import_done", applied), COLOR_OK);
    }

    private static Text layoutText(MemePadConfig.Layout layout) {
        return Text.translatable(layout == MemePadConfig.Layout.TILES
                ? "ygmemepad.settings.layout_tiles" : "ygmemepad.settings.layout_list");
    }

    private void onResetBindsPressed() {
        if (!resetArmed) {
            setResetArmed(true);
            return;
        }
        int count = MemePadConfig.get().getBindingsCount();
        MemePadConfig.get().clearAllBindings();
        setResetArmed(false);
        showStatus(Text.translatable("ygmemepad.settings.binds_reset", count), COLOR_OK);
    }

    private void setResetArmed(boolean armed) {
        resetArmed = armed;
        resetArmedAt = System.currentTimeMillis();

        MemePadConfig config = MemePadConfig.get();
        if (armed) {
            resetBindsButton.setMessage(Text.translatable("ygmemepad.settings.reset_confirm"));
            resetBindsButton.renderer(ButtonComponent.Renderer.flat(C_DANGER, C_DANGER_HOVER, C_OFF));
            statusLabel.text(Text.empty());
        } else {
            resetBindsButton.setMessage(Text.translatable("ygmemepad.settings.reset_binds"));
            resetBindsButton.renderer(ButtonComponent.Renderer.flat(
                    config.getColorArgb(MemePadTheme.Slot.BUTTON),
                    config.getColorHoverArgb(MemePadTheme.Slot.BUTTON), C_OFF));
        }
    }

    private static ButtonComponent button(Text text, int width, int height, int color, int hover,
                                          Consumer<ButtonComponent> onPress) {
        ButtonComponent button = UIComponents.button(text, onPress);
        button.renderer(ButtonComponent.Renderer.flat(color, hover, C_OFF));
        button.sizing(Sizing.fixed(width), Sizing.fixed(height));
        return button;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (resetArmed && System.currentTimeMillis() - resetArmedAt > CONFIRM_TIMEOUT_MS) {
            setResetArmed(false);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        MemePadConfig.get().flush();
        super.removed();
    }

    @Override
    public void close() {
        MemePadConfig.get().flush();
        if (this.client != null) {
            this.client.setScreen(returnTo.get());
        }
    }
}
