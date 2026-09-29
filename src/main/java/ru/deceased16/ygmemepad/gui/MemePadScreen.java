package ru.deceased16.ygmemepad.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.deceased16.ygmemepad.YGMemePadClient;
import ru.deceased16.ygmemepad.config.MemePadConfig;
import ru.deceased16.ygmemepad.network.MemeSoundEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MemePadScreen extends Screen {

    private static final int ROWS_PER_PAGE = 7;
    private static final int ROW_HEIGHT = 20;
    private static final int LIST_TOP_MARGIN = 66;

    private final Screen parent;

    private List<Object> rows = new ArrayList<>();
    private int page = 0;
    private int lastSeenDataVersion = -1;

    private String awaitingKeyFor = null;
    private static final String OPEN_MENU_MARKER = "\0open_menu";

    public MemePadScreen(Screen parent) {
        super(Text.translatable("ygmemepad.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        YGMemePadClient.requestSoundListRefresh();
        buildRows();
        rebuild();
    }

    private void buildRows() {
        rows.clear();

        Map<String, List<MemeSoundEntry>> byCategory = new LinkedHashMap<>();
        for (MemeSoundEntry sound : YGMemePadClient.getLastSoundData().sounds()) {
            String category = sound.hasCategory() ? sound.category() : "—";
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(sound);
        }

        for (Map.Entry<String, List<MemeSoundEntry>> entry : byCategory.entrySet()) {
            rows.add(entry.getKey());
            rows.addAll(entry.getValue());
        }

        lastSeenDataVersion = YGMemePadClient.getDataVersion();
    }

    private void rebuild() {
        clearChildren();

        int centerX = this.width / 2;

        int openMenuKey = MemePadConfig.get().getOpenMenuKey();
        String openMenuKeyLabel = keyLabel(openMenuKey);
        boolean recordingOpenMenu = OPEN_MENU_MARKER.equals(awaitingKeyFor);

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("ygmemepad.screen.open_menu_key", openMenuKeyLabel),
                        b -> startRecording(OPEN_MENU_MARKER))
                .dimensions(centerX - 160, 36, 320, 20)
                .build());

        int totalPages = Math.max(1, (rows.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = page * ROWS_PER_PAGE;
        int endIndex = Math.min(startIndex + ROWS_PER_PAGE, rows.size());

        for (int i = startIndex; i < endIndex; i++) {
            int rowY = LIST_TOP_MARGIN + (i - startIndex) * ROW_HEIGHT;
            Object row = rows.get(i);

            if (row instanceof String) {
                continue;
            }

            MemeSoundEntry sound = (MemeSoundEntry) row;
            addSoundRow(sound, rowY, centerX);
        }

        int bottomY = LIST_TOP_MARGIN + ROWS_PER_PAGE * ROW_HEIGHT + 10;

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                    page--;
                    rebuild();
                })
                .dimensions(centerX - 160, bottomY, 20, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                    page++;
                    rebuild();
                })
                .dimensions(centerX + 140, bottomY, 20, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("ygmemepad.screen.refresh"), b -> {
                    YGMemePadClient.requestSoundListRefresh();
                })
                .dimensions(centerX - 135, bottomY, 275, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(centerX - 100, bottomY + 26, 200, 20)
                .build());
    }

    private void addSoundRow(MemeSoundEntry sound, int rowY, int centerX) {
        boolean recordingThis = sound.name().equals(awaitingKeyFor);

        addDrawableChild(ButtonWidget.builder(Text.literal(sound.name()), b -> playPreview(sound.name()))
                .dimensions(centerX - 160, rowY, 170, 18)
                .build());

        Integer boundKey = MemePadConfig.get().getKeyForSound(sound.name());
        Text keyButtonText = recordingThis
                ? Text.translatable("ygmemepad.screen.press_key")
                : Text.literal(boundKey != null ? keyLabel(boundKey) : Text.translatable("ygmemepad.screen.assign").getString());

        addDrawableChild(ButtonWidget.builder(keyButtonText, b -> startRecording(sound.name()))
                .dimensions(centerX + 15, rowY, 105, 18)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("ygmemepad.screen.clear"), b -> {
                    MemePadConfig.get().clearKeyForSound(sound.name());
                    rebuild();
                })
                .dimensions(centerX + 125, rowY, 35, 18)
                .build());
    }

    private void startRecording(String target) {
        this.awaitingKeyFor = target;
        rebuild();
    }

    private void playPreview(String soundName) {
        if (this.client != null && this.client.player != null) {
            this.client.player.networkHandler.sendChatCommand("memplay \"" + soundName + "\"");
        }
    }

    private static String keyLabel(int glfwKeyCode) {
        if (glfwKeyCode < 0) {
            return Text.translatable("ygmemepad.screen.not_set").getString();
        }
        return InputUtil.Type.KEYSYM.createFromCode(glfwKeyCode).getLocalizedText().getString();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (awaitingKeyFor != null) {
            int keyCode = input.key();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                awaitingKeyFor = null;
            } else if (OPEN_MENU_MARKER.equals(awaitingKeyFor)) {
                MemePadConfig.get().setOpenMenuKey(keyCode);
                awaitingKeyFor = null;
            } else {
                MemePadConfig.get().setKeyForSound(awaitingKeyFor, keyCode);
                awaitingKeyFor = null;
            }
            rebuild();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (YGMemePadClient.getDataVersion() != lastSeenDataVersion) {
            buildRows();
            rebuild();
        }

        context.fill(0, 0, this.width, this.height, 0xC0101010);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);

        int totalPages = Math.max(1, (rows.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        int startIndex = page * ROWS_PER_PAGE;
        int endIndex = Math.min(startIndex + ROWS_PER_PAGE, rows.size());

        for (int i = startIndex; i < endIndex; i++) {
            Object row = rows.get(i);
            if (row instanceof String categoryHeader) {
                int rowY = LIST_TOP_MARGIN + (i - startIndex) * ROW_HEIGHT;
                context.drawTextWithShadow(this.textRenderer, categoryHeader, this.width / 2 - 160, rowY + 5, 0xFFD54F);
            }
        }

        if (rows.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("ygmemepad.screen.no_sounds"),
                    this.width / 2,
                    this.height - 46,
                    0xFFAA00
            );
        } else {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal((page + 1) + " / " + totalPages),
                    this.width / 2,
                    this.height - 46,
                    0xAAAAAA
            );
        }
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
