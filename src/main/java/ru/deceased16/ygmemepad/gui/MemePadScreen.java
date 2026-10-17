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
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import ru.deceased16.ygmemepad.YGMemePadClient;
import ru.deceased16.ygmemepad.config.MemePadConfig;
import ru.deceased16.ygmemepad.config.MemePadTheme;
import ru.deceased16.ygmemepad.input.KeyConflicts;
import ru.deceased16.ygmemepad.network.ActionResult;
import ru.deceased16.ygmemepad.network.MemePadActions;
import ru.deceased16.ygmemepad.network.MemeSoundEntry;
import ru.deceased16.ygmemepad.network.MemeSoundListData;
import ru.deceased16.ygmemepad.update.UpdateManager;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class MemePadScreen extends BaseOwoScreen<FlowLayout> {

    private static final int SIDEBAR_W = 130;
    private static final int TILE_GAP = 6;
    private static final int TILE_MIN_W = 80;
    private static final long RESULT_SHOW_MS = 6000;
    private static final int C_OFF = 0x60101418;
    private static final int TILE_PAD = 3;
    private static final int LIST_KEY_W = 110;
    private static final int LIST_CLEAR_W = 20;
    private static final int C_DANGER = 0xD0A83232;
    private static final int C_DANGER_HOVER = 0xE0C43E3E;
    private final Screen parent;
    private final boolean listMode = MemePadConfig.get().getLayout() == MemePadConfig.Layout.LIST;
    private String selectedTab;
    private String query;
    private boolean deleteMode = false;
    private String awaitingKeyFor = null;
    private int lastSeenDataVersion = -1;
    private int lastBuiltWidth = -1;
    private int lastSeenResultVersion = 0;
    private long resultShownAt = 0;
    private String shownStatus = "";
    private boolean dirtyTabs = true;
    private boolean dirtyTiles = true;
    private boolean dirtyAdmin = true;
    private boolean dirtyUpdate = true;
    private int lastSeenUpdateCounter = -1;
    private FlowLayout tabsFlow;
    private FlowLayout tilesFlow;
    private FlowLayout adminRow;
    private FlowLayout updateRow;
    private TextBoxComponent searchBox;
    private ButtonComponent sortButton;
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
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
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

        sortButton = button(sortText(MemePadConfig.get().getSortMode()), sortButtonWidth(), 20,
                cNormal(), cNormalHover(), b -> {
                    MemePadConfig.SortMode next = MemePadConfig.get().getSortMode().next();
                    MemePadConfig.get().setSortMode(next);
                    sortButton.setMessage(sortText(next));
                    dirtyTiles = true;
                });
        header.child(sortButton);
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

        tilesFlow = listMode
                ? UIContainers.verticalFlow(Sizing.fill(), Sizing.content())
                : UIContainers.ltrTextFlow(Sizing.fill(), Sizing.content());
        tilesFlow.gap(listMode ? 3 : TILE_GAP);
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

        updateRow = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        updateRow.gap(6);
        updateRow.verticalAlignment(VerticalAlignment.CENTER);
        footer.child(updateRow);

        FlowLayout buttons = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        buttons.gap(4);
        buttons.verticalAlignment(VerticalAlignment.CENTER);
        adminRow = UIContainers.horizontalFlow(Sizing.expand(), Sizing.content());
        adminRow.gap(3);
        adminRow.verticalAlignment(VerticalAlignment.CENTER);
        buttons.child(adminRow);

        buttons.child(button(Text.translatable("ygmemepad.screen.settings"), 90, 20, cNormal(), cNormalHover(),
                b -> openScreen(new MemePadSettingsScreen(this::reopen))));
        buttons.child(button(Text.translatable("ygmemepad.screen.refresh_short"), 90, 20, cNormal(), cNormalHover(),
                b -> YGMemePadClient.requestSoundListRefresh()));
        buttons.child(button(Text.translatable("gui.done"), 80, 20, cNormal(), cNormalHover(), b -> close()));
        footer.child(buttons);

        root.child(footer);
    }

    private static int cNormal() {
        return MemePadConfig.get().getColorArgb(MemePadTheme.Slot.BUTTON);
    }

    private static int cNormalHover() {
        return MemePadConfig.get().getColorHoverArgb(MemePadTheme.Slot.BUTTON);
    }

    private static int cKey() {
        return MemePadConfig.get().getColorArgb(MemePadTheme.Slot.KEY);
    }

    private static int cKeyHover() {
        return MemePadConfig.get().getColorHoverArgb(MemePadTheme.Slot.KEY);
    }

    private static int cAccent() {
        return MemePadConfig.get().getColorArgb(MemePadTheme.Slot.ACCENT);
    }

    private static int cAccentHover() {
        return MemePadConfig.get().getColorHoverArgb(MemePadTheme.Slot.ACCENT);
    }

    private ButtonComponent compactButton(Text text, int color, int hover, Consumer<ButtonComponent> onPress) {
        return button(text, this.textRenderer.getWidth(text) + 14, 16, color, hover, onPress);
    }

    private static ButtonComponent button(Text text, int width, int height, int color, int hover,
                                          Consumer<ButtonComponent> onPress) {
        ButtonComponent button = UIComponents.button(text, onPress);
        button.renderer(ButtonComponent.Renderer.flat(color, hover, C_OFF));
        button.sizing(Sizing.fixed(width), Sizing.fixed(height));
        return button;
    }

    private static Text sortText(MemePadConfig.SortMode mode) {
        return Text.translatable("ygmemepad.settings.sort",
                Text.translatable("ygmemepad.sort." + mode.name().toLowerCase(Locale.ROOT)));
    }

    private int sortButtonWidth() {
        int widest = 0;
        for (MemePadConfig.SortMode mode : MemePadConfig.SortMode.values()) {
            widest = Math.max(widest, this.textRenderer.getWidth(sortText(mode)));
        }
        return widest + 16;
    }

    private int tileWidth() {
        MemePadConfig config = MemePadConfig.get();
        int columns = config.getColumns();
        if (columns <= 0) {
            return config.getTileWidth();
        }
        int available = this.width - 2 * 8 - SIDEBAR_W - 6 - 2 * 6 - 6 - 4;
        int width = (available - (columns - 1) * TILE_GAP) / columns;
        return Math.max(TILE_MIN_W, width);
    }

    private void applyPending() {
        MemeSoundListData data = YGMemePadClient.getLastSoundData();

        if (this.width != lastBuiltWidth) {
            lastBuiltWidth = this.width;
            dirtyTiles = true;
        }

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
        if (UpdateManager.getChangeCounter() != lastSeenUpdateCounter) {
            lastSeenUpdateCounter = UpdateManager.getChangeCounter();
            dirtyUpdate = true;
        }
        if (dirtyUpdate) {
            dirtyUpdate = false;
            rebuildUpdateRow();
        }
        updateStatus();
    }

    private void rebuildUpdateRow() {
        updateRow.clearChildren();
        UpdateManager.State updateState = UpdateManager.getState();

        switch (updateState) {
            case AVAILABLE, FAILED -> {
                boolean failed = updateState == UpdateManager.State.FAILED;
                LabelComponent label = UIComponents.label(failed
                        ? Text.translatable("ygmemepad.update.failed", UpdateManager.getError())
                        : Text.translatable("ygmemepad.update.available", UpdateManager.getLatestVersion()));
                label.color(Color.ofArgb(failed ? 0xFFFF5555 : 0xFFFFD54F));
                updateRow.child(label);
                updateRow.child(button(Text.translatable(failed ? "ygmemepad.update.retry" : "ygmemepad.update.button"),
                        90, 18, cAccent(), cAccentHover(), b -> UpdateManager.install()));
            }
            case DOWNLOADING -> {
                LabelComponent label = UIComponents.label(
                        Text.translatable("ygmemepad.update.downloading", UpdateManager.getPercent() + "%"));
                label.color(Color.ofArgb(0xFFFFD54F));
                updateRow.child(label);
            }
            case READY -> {
                LabelComponent label = UIComponents.label(Text.translatable("ygmemepad.update.ready"));
                label.color(Color.ofArgb(0xFF55FF55));
                updateRow.child(label);
            }
            default -> {
            }
        }
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
                    selected ? cAccent() : cNormal(),
                    selected ? cAccentHover() : cNormalHover(),
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
        return listMode ? createRow(sound, admin) : createTileCard(sound, admin);
    }

    private FlowLayout createRow(MemeSoundEntry sound, boolean admin) {
        String name = sound.name();

        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(), Sizing.content());
        row.gap(4);
        row.padding(Insets.of(TILE_PAD));
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.surface(Surface.flat(0x60000000).and(Surface.outline(0x28FFFFFF)));

        boolean deleting = deleteMode && admin;
        int nameMaxPx = Math.max(80, this.width - 2 * 8 - SIDEBAR_W - 6 - 12 - 8 - 2 * TILE_PAD
                - 2 * 4 - LIST_KEY_W - LIST_CLEAR_W - 16);
        ButtonComponent nameButton = button(Text.literal(fit(name, nameMaxPx)), 100, 20,
                deleting ? C_DANGER : cNormal(),
                deleting ? C_DANGER_HOVER : cNormalHover(),
                b -> onTileClick(name));
        nameButton.horizontalSizing(Sizing.expand());
        nameButton.tooltip(Text.literal(name));
        row.child(nameButton);

        addKeyControls(row, name, LIST_KEY_W, 20, LIST_CLEAR_W, 20);
        return row;
    }

    private void addKeyControls(FlowLayout target, String name, int keyWidth, int keyHeight,
                                int clearWidth, int clearHeight) {
        boolean recording = name.equals(awaitingKeyFor);
        Integer boundKey = MemePadConfig.get().getKeyForSound(name);

        List<String> conflicts = boundKey == null
                ? List.of() : KeyConflicts.find(this.client, name, boundKey);
        boolean conflict = !recording && !conflicts.isEmpty();

        Text keyText;
        if (recording) {
            keyText = Text.literal(fit(Text.translatable("ygmemepad.screen.press_key").getString(), keyWidth - 8))
                    .formatted(Formatting.YELLOW);
        } else if (boundKey != null) {
            keyText = Text.literal(fit(keyLabel(boundKey), keyWidth - 8))
                    .formatted(conflict ? Formatting.WHITE : Formatting.AQUA);
        } else {
            keyText = Text.literal(fit(Text.translatable("ygmemepad.screen.assign").getString(), keyWidth - 8))
                    .formatted(Formatting.GRAY);
        }

        ButtonComponent keyButton = button(keyText, keyWidth, keyHeight,
                recording ? cAccent() : conflict ? C_DANGER : cKey(),
                recording ? cAccentHover() : conflict ? C_DANGER_HOVER : cKeyHover(),
                b -> {
                    awaitingKeyFor = name;
                    dirtyTiles = true;
                });
        if (conflict) {
            keyButton.tooltip(Text.translatable("ygmemepad.conflict.tooltip", String.join(", ", conflicts)));
        }
        target.child(keyButton);

        ButtonComponent clearButton = button(Text.literal("×"), clearWidth, clearHeight, cKey(), cKeyHover(), b -> {
            MemePadConfig.get().clearKeyForSound(name);
            dirtyTiles = true;
        });
        clearButton.active(boundKey != null);
        target.child(clearButton);
    }

    private FlowLayout createTileCard(MemeSoundEntry sound, boolean admin) {
        String name = sound.name();
        int tileW = tileWidth();
        int inner = tileW - 2 * TILE_PAD;

        FlowLayout tile = UIContainers.verticalFlow(Sizing.fixed(tileW), Sizing.content());
        tile.gap(3);
        tile.padding(Insets.of(TILE_PAD));
        tile.margins(Insets.bottom(TILE_GAP));
        tile.surface(Surface.flat(0x60000000).and(Surface.outline(0x28FFFFFF)));

        boolean deleting = deleteMode && admin;
        ButtonComponent nameButton = button(Text.literal(fit(name, inner - 8)), inner, 20,
                deleting ? C_DANGER : cNormal(),
                deleting ? C_DANGER_HOVER : cNormalHover(),
                b -> onTileClick(name));
        nameButton.tooltip(Text.literal(name));
        tile.child(nameButton);

        FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(inner), Sizing.content());
        row.gap(2);
        addKeyControls(row, name, inner - 22, 16, 20, 16);

        tile.child(row);
        return tile;
    }

    private void rebuildAdminRow(MemeSoundListData data) {
        adminRow.clearChildren();
        if (!data.admin()) {
            return;
        }

        adminRow.child(compactButton(Text.translatable("ygmemepad.admin.add_sound"), cNormal(), cNormalHover(),
                b -> openScreen(new AddSoundScreen(this::reopen, currentCategoryOrNull()))));

        adminRow.child(compactButton(Text.translatable("ygmemepad.admin.add_category"), cNormal(), cNormalHover(),
                b -> openScreen(new AddCategoryScreen(this::reopen))));

        adminRow.child(compactButton(Text.translatable(deleteMode
                        ? "ygmemepad.admin.delete_mode_on" : "ygmemepad.admin.delete_mode_off"),
                deleteMode ? C_DANGER : cNormal(),
                deleteMode ? C_DANGER_HOVER : cNormalHover(),
                b -> {
                    deleteMode = !deleteMode;
                    dirtyTiles = true;
                    dirtyAdmin = true;
                }));

        if (currentCategoryOrNull() != null) {
            adminRow.child(compactButton(Text.translatable("ygmemepad.admin.delete_category"),
                    cNormal(), cNormalHover(), b -> confirmRemoveCategory()));
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
            MemePadConfig.SortMode mode = MemePadConfig.get().getSortMode();
            if (mode == MemePadConfig.SortMode.MOST_USED || mode == MemePadConfig.SortMode.RECENT) {
                dirtyTiles = true;
            }
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
        if (glfwKeyCode >= GLFW.GLFW_KEY_KP_0 && glfwKeyCode <= GLFW.GLFW_KEY_KP_9) {
            return "NUM-" + (glfwKeyCode - GLFW.GLFW_KEY_KP_0);
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
