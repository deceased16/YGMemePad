package ru.deceased16.ygmemepad;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import ru.deceased16.ygmemepad.input.MemePadInput;
import ru.deceased16.ygmemepad.input.MemePadKeys;
import ru.deceased16.ygmemepad.network.ActionResult;
import ru.deceased16.ygmemepad.network.MemePadActions;
import ru.deceased16.ygmemepad.network.MemeSoundListData;
import ru.deceased16.ygmemepad.network.MemeSoundListDecoder;
import ru.deceased16.ygmemepad.network.RawPayload;
import ru.deceased16.ygmemepad.network.ServerMessage;
import ru.deceased16.ygmemepad.update.UpdateManager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YGMemePadClient implements ClientModInitializer {

    private static final Logger LOGGER = Logger.getLogger("YGMemePad");

    private static volatile MemeSoundListData lastSoundData = MemeSoundListData.EMPTY;
    private static volatile ActionResult lastResult = null;

    private static final AtomicInteger dataVersion = new AtomicInteger(0);
    private static final AtomicInteger resultVersion = new AtomicInteger(0);

    @Override
    public void onInitializeClient() {
        MemePadKeys.register();
        UpdateManager.start();

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                client.execute(UpdateManager::promptIfPossible);
            }
        });

        PayloadTypeRegistry.playS2C().register(RawPayload.ID, RawPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RawPayload.ID, RawPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(RawPayload.ID, (payload, context) ->
                context.client().execute(() -> handleIncoming(payload.data())));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            requestSoundListRefresh();
            UpdateManager.notifyIfPossible();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastSoundData = MemeSoundListData.EMPTY;
            lastResult = null;
            dataVersion.incrementAndGet();
        });

        ClientTickEvents.END_CLIENT_TICK.register(MemePadInput::onTick);
    }

    private static void handleIncoming(byte[] data) {
        try {
            ServerMessage message = MemeSoundListDecoder.decode(data);
            if (message instanceof MemeSoundListData list) {
                lastSoundData = list;
                dataVersion.incrementAndGet();
            } else if (message instanceof ActionResult result) {
                lastResult = result;
                resultVersion.incrementAndGet();
                showOverlay(result);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Не удалось разобрать сообщение от сервера", e);
        }
    }

    private static void showOverlay(ActionResult result) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) {
            client.inGameHud.setOverlayMessage(
                    Text.literal(result.message()).formatted(result.success() ? Formatting.GREEN : Formatting.RED),
                    false);
        }
    }

    public static void requestSoundListRefresh() {
        MemePadActions.requestSoundList();
    }

    public static MemeSoundListData getLastSoundData() {
        return lastSoundData;
    }

    public static int getDataVersion() {
        return dataVersion.get();
    }

    public static ActionResult getLastResult() {
        return lastResult;
    }

    public static int getResultVersion() {
        return resultVersion.get();
    }
}
