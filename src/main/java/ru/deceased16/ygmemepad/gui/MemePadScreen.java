package ru.deceased16.ygmemepad.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
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
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import ru.deceased16.ygmemepad.YGMemePadClient;
import ru.deceased16.ygmemepad.config.MemePadConfig;
import ru.deceased16.ygmemepad.network.ActionResult;
import ru.deceased16.ygmemepad.network.MemePadActions;
import ru.deceased16.ygmemepad.network.MemeSoundEntry;
import ru.deceased16.ygmemepad.network.MemeSoundListData;

import java.util.List;
import java.util.function.Consumer;

public class MemePadScreen extends BaseOwoScreen<FlowLayout> {

    private static final int SIDEBAR_W = 130;
    private static final int TILE_W = 112;
    private static final long RESULT_SHOW_MS = 6000;
    private static final int C_NORMAL = 0xB0202630;
    private static final int C_NORMAL_HOVER = 0xC03A4658;
    private static final int C_OFF = 0x60101418;
    private static final int C_KEY = 0xB0141A2A;
    private static final int C_KEY_HOVER = 0xC0283A58;
    private static final int TILE_PAD = 3;
    private static final int C_ACCENT = 0xD03A78C2;
    private static final int C_ACCENT_HOVER = 0xE04A8AD6;
    private static final int C_DANGER = 0xD0A83232;
    private static final int C_DANGER_HOVER = 0xE0C43E3E;
    private final Screen parent;
    private String selectedTab;
    private String query;
    private boolean deleteMode = false;
    private String awaitingKeyFor = null;
    private int lastSeenDataVersion = -1;
    private int lastSeenResultVersion = 0;
    private long resultShownAt = 0;
    private String shownStatus = "";
    private boolean dirtyTabs = true;
    private boolean dirtyTiles = true;
    private boolean dirtyAdmin = true;
    private FlowLayout tabsFlow;
    private FlowLayout tilesFlow;
    private FlowLayout adminRow;
    private TextBoxComponent searchBox;
    private LabelComponent statusLabel;
    private LabelComponent cooldownLabel;

    public MemePadScreen(Screen parent) {
        this(parent, MemePadModel.TAB_ALL, "");
    }

    private MemePadScreen(Screen parent, String selectedTab, String query) {
        super(Text.translatable("ygmemepad.screen.title"));
        this.parent = parent;
        this.selectedTab = selectedTab;
        this.query = query;
    }

    private Screen reopen() {
        return new MemePadScreen(parent, selectedTab, query);
    }

    @Override
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        YGMemePadClient.requestSoundListRefresh();

        root.surface(Surface.flat(0xC0101010));
        root.padding(Insets.of(8));
        root.gap(6);

        FlowLayout header = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        header.gap(10);
        header.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent title = UIComponents.label(this.title);
        title.shadow(true);
        header.child(title);

        searchBox = UIComponents.textBox(Sizing.fixed(220));
        searchBox.setMaxLength(64);
        searchBox.setPlaceholder(Text.translatable("ygmemepad.screen.search"));
        searchBox.text(query);
        searchBox.onChanged().subscribe(text -> {
            query = text;
            dirtyTiles = true;
        });
        header.child(searchBox);
        root.child(header);

        FlowLayout body = UIContainers.horizontalFlow(Sizing.fill(), Sizing.expand());
        body.gap(6);

        tabsFlow = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
        tabsFlow.gap(2);
        tabsFlow.padding(Insets.of(4));
        ScrollContainer<FlowLayout> sidebar = UIContainers.verticalScroll(Sizing.fixed(SIDEBAR_W), Sizing.fill(), tabsFlow);
        sidebar.surface(Surface.flat(0x90000000));
        sidebar.scrollbar(ScrollContainer.Scrollbar.flat(Color.WHITE));
        body.child(sidebar);

        tilesFlow = UIContainers.ltrTextFlow(Sizing.fill(), Sizing.content());
        tilesFlow.gap(6);
        tilesFlow.padding(Insets.of(6));
        ScrollContainer<FlowLayout> main = UIContainers.verticalScroll(Sizing.expand(), Sizing.fill(), tilesFlow);
        main.surface(Surface.flat(0x70000000));
        main.scrollbar(ScrollContainer.Scrollbar.flat(Color.WHITE));
        main.scrollbarThiccness(6);
        body.child(main);

        root.child(body);

        FlowLayout footer = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
        footer.gap(4);

        FlowLayout statusRow = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        statusLabel = UIComponents.label(Text.empty());
        statusLabel.horizontalSizing(Sizing.expand());
        cooldownLabel = UIComponents.label(Text.empty());
        cooldownLabel.color(Color.ofArgb(0xFFAAAAAA));
        statusRow.child(statusLabel);
        statusRow.child(cooldownLabel);
        footer.child(statusRow);

        adminRow = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        adminRow.gap(4);
        footer.child(adminRow);

        FlowLayout buttons = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        buttons.gap(4);
        buttons.horizontalAlignment(HorizontalAlignment.RIGHT);
        buttons.child(button(Text.translatable("ygmemepad.screen.refresh_short"), 90, 20, C_NORMAL, C_NORMAL_HOVER,
                b -> YGMemePadClient.requestSoundListRefresh()));
        buttons.child(button(Text.translatable("gui.done"), 80, 20, C_NORMAL, C_NORMAL_HOVER, b -> close()));
        footer.child(buttons);

        root.child(footer);
    }

    private static ButtonComponent button(Text text, int width, int height, int color, int hover,
                                          Consumer<ButtonComponent> onPress) {
        ButtonComponent button = UIComponents.button(text, onPress);
        button.renderer(ButtonComponent.Renderer.flat(color, hover, C_OFF));
        button.sizing(Sizing.fixed(width), Sizing.fixed(height));
        return button;
    }

    private void applyPending() {
        MemeSoundListData data = YGMemePadClient.getLastSoundData();

        if (YGMemePadClient.getDataVersion() != lastSeenDataVersion) {
            lastSeenDataVersion = YGMemePadClient.getDataVersion();
            selectedTab = MemePadModel.validTab(data, selectedTab);
            if (!data.admin()) {
                deleteMode = false;
            }
            dirtyTabs = true;
            dirtyTiles = true;
            dirtyAdmin = true;
            cooldownLabel.text(data.cooldownSeconds() > 0
                    ? Text.translatable("ygmemepad.screen.cooldown", data.cooldownSeconds())
                    : Text.translatable("ygmemepad.screen.cooldown_none"));
        }

        if (dirtyTabs) {
            dirtyTabs = false;
            rebuildTabs(data);
        }
        if (dirtyTiles) {
            dirtyTiles = false;
            rebuildTiles(data);
        }
        if (dirtyAdmin) {
            dirtyAdmin = false;
            rebuildAdminRow(data);
        }
        updateStatus();
    }

    private void rebuildTabs(MemeSoundListData data) {
        tabsFlow.clearChildren();

        for (MemePadModel.Tab tab : MemePadModel.tabs(data)) {
            String name;
            if (tab.isAll()) {
                name = Text.translatable("ygmemepad.tab.all").getString();
            } else if (tab.isUncategorized()) {
                name = Text.translatable("ygmemepad.tab.uncategorized").getString();
            } else {
                name = tab.id();
            }

            boolean selected = tab.id().equals(selectedTab);
            ButtonComponent button = button(Text.literal(fit(name + " (" + tab.count() + ")", SIDEBAR_W - 8 - 14)),
                    SIDEBAR_W - 8 - 8, 20,
                    selected ? C_ACCENT : C_NORMAL,
                    selected ? C_ACCENT_HOVER : C_NORMAL_HOVER,
                    b -> selectTab(tab.id()));
            button.sizing(Sizing.fill(), Sizing.fixed(20));
            tabsFlow.child(button);
        }
    }

    private void selectTab(String id) {
        selectedTab = id;
        if (!query.isEmpty()) {
            query = "";
            searchBox.text("");
        }
        dirtyTabs = true;
        dirtyTiles = true;
        dirtyAdmin = true;
    }

    private void rebuildTiles(MemeSoundListData data) {
        tilesFlow.clearChildren();

        List<MemeSoundEntry> sounds = MemePadModel.filter(data, selectedTab, query, true);

        if (sounds.isEmpty()) {
            Text hint = Text.translatable(data.sounds().isEmpty()
                    ? "ygmemepad.screen.no_sounds" : "ygmemepad.screen.nothing_found");
            LabelComponent label = UIComponents.label(hint);
            label.color(Color.ofArgb(0xFFAAAAAA));
            tilesFlow.child(label);
            return;
        }

        for (MemeSoundEntry sound : sounds) {
            tilesFlow.child(createTile(sound, data.admin()));
        }
    }

    private FlowLayout createTile(MemeSoundEntry sound, boolean admin) {
        String name = sound.name();
        int inner = TILE_W - 2 * TILE_PAD;

        FlowLayout tile = UIContainers.verticalFlow(Sizing.fixed(TILE_W), Sizing.content());
        tile.gap(3);
        tile.padding(Insets.of(TILE_PAD));
        tile.margins(Insets.bottom(6));
        tile.surface(Surface.flat(0x60000000).and(Surface.outline(0x28FFFFFF)));

        boolean deleting = deleteMode && admin;
        ButtonComponent nameButton = button(Text.literal(fit(name, inner - 8)), inner, 20,
                deleting ? C_DANGER : C_NORMAL,
                deleting ? C_DANGER_HOVER : C_NORMAL_HOVER,
                b -> onTileClick(name));
        nameButton.tooltip(Text.literal(name));
        tile.child(nameButton);

        boolean recording = name.equals(awaitingKeyFor);
        Integer boundKey = MemePadConfig.get().getKeyForSound(name);

        Text keyText;
        if (recording) {
            keyText = Text.translatable("ygmemepad.screen.press_key").formatted(Formatting.YELLOW);
        } else if (boundKey != null) {
            keyText = Text.literal(keyLabel(boundKey)).formatted(Formatting.AQUA);
        } else {
            keyText = Text.translatable("ygmemepad.screen.assign").formatted(Formatting.GRAY);
        }

        FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(inner), Sizing.content());
        row.gap(2);

        ButtonComponent keyButton = button(keyText, inner - 22, 16,
                recording ? C_ACCENT : C_KEY,
                recording ? C_ACCENT_HOVER : C_KEY_HOVER,
                b -> {
                    awaitingKeyFor = name;
                    dirtyTiles = true;
                });
        row.child(keyButton);

        ButtonComponent clearButton = button(Text.literal("×"), 20, 16, C_KEY, C_KEY_HOVER, b -> {
            MemePadConfig.get().clearKeyForSound(name);
            dirtyTiles = true;
        });
        clearButton.active(boundKey != null);
        row.child(clearButton);

        tile.child(row);
        return tile;
    }

    private void rebuildAdminRow(MemeSoundListData data) {
        adminRow.clearChildren();
        if (!data.admin()) {
            return;
        }

        adminRow.child(button(Text.translatable("ygmemepad.admin.add_sound"), 100, 20, C_NORMAL, C_NORMAL_HOVER,
                b -> openScreen(new AddSoundScreen(this::reopen, currentCategoryOrNull()))));

        adminRow.child(button(Text.translatable("ygmemepad.admin.add_category"), 100, 20, C_NORMAL, C_NORMAL_HOVER,
                b -> openScreen(new AddCategoryScreen(this::reopen))));

        adminRow.child(button(Text.translatable(deleteMode
                        ? "ygmemepad.admin.delete_mode_on" : "ygmemepad.admin.delete_mode_off"),
                120, 20,
                deleteMode ? C_DANGER : C_NORMAL,
                deleteMode ? C_DANGER_HOVER : C_NORMAL_HOVER,
                b -> {
                    deleteMode = !deleteMode;
                    dirtyTiles = true;
                    dirtyAdmin = true;
                }));

        if (currentCategoryOrNull() != null) {
            adminRow.child(button(Text.translatable("ygmemepad.admin.delete_category"), 130, 20,
                    C_NORMAL, C_NORMAL_HOVER, b -> confirmRemoveCategory()));
        }
    }

    private void updateStatus() {
        if (YGMemePadClient.getResultVersion() != lastSeenResultVersion) {
            lastSeenResultVersion = YGMemePadClient.getResultVersion();
            resultShownAt = System.currentTimeMillis();
        }

        ActionResult result = YGMemePadClient.getLastResult();
        boolean visible = result != null && System.currentTimeMillis() - resultShownAt < RESULT_SHOW_MS;
        String key = visible ? (result.success() ? "+" : "-") + result.message() : "";

        if (!key.equals(shownStatus)) {
            shownStatus = key;
            if (visible) {
                statusLabel.text(Text.literal(result.message()));
                statusLabel.color(Color.ofArgb(result.success() ? 0xFF55FF55 : 0xFFFF5555));
            } else {
                statusLabel.text(Text.empty());
            }
        }
    }

    private String currentCategoryOrNull() {
        if (MemePadModel.TAB_ALL.equals(selectedTab) || MemePadModel.TAB_UNCATEGORIZED.equals(selectedTab)) {
            return null;
        }
        return selectedTab;
    }

    private void onTileClick(String soundName) {
        if (deleteMode && YGMemePadClient.getLastSoundData().admin()) {
            confirm(Text.translatable("ygmemepad.confirm.remove_sound", soundName),
                    () -> MemePadActions.removeSound(soundName));
        } else if (this.client != null && this.client.player != null) {
            MemePadActions.play(this.client, soundName);
        }
    }

    private void confirmRemoveCategory() {
        String category = currentCategoryOrNull();
        if (category == null) {
            return;
        }
        confirm(Text.translatable("ygmemepad.confirm.remove_category", category),
                () -> MemePadActions.removeCategory(category));
    }

    private void confirm(Text message, Runnable onYes) {
        if (this.client == null) {
            return;
        }
        this.client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                onYes.run();
            }
            this.client.setScreen(reopen());
        }, Text.translatable("ygmemepad.confirm.title"), message));
    }

    private void openScreen(Screen screen) {
        if (this.client != null) {
            this.client.setScreen(screen);
        }
    }

    private static String keyLabel(int glfwKeyCode) {
        if (glfwKeyCode < 0) {
            return Text.translatable("ygmemepad.screen.not_set").getString();
        }
        return InputUtil.Type.KEYSYM.createFromCode(glfwKeyCode).getLocalizedText().getString();
    }

    private String fit(String text, int maxPixels) {
        if (this.textRenderer.getWidth(text) <= maxPixels) {
            return text;
        }
        int ellipsis = this.textRenderer.getWidth("…");
        return this.textRenderer.trimToWidth(text, Math.max(0, maxPixels - ellipsis)) + "…";
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (awaitingKeyFor != null) {
            int keyCode = input.key();
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                MemePadConfig.get().setKeyForSound(awaitingKeyFor, keyCode);
            }
            awaitingKeyFor = null;
            dirtyTiles = true;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        applyPending();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
