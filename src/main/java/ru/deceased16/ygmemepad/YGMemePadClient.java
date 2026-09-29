package ru.deceased16.ygmemepad;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.deceased16.ygmemepad.input.MemePadInput;
import ru.deceased16.ygmemepad.network.MemeSoundListData;
import ru.deceased16.ygmemepad.network.MemeSoundListDecoder;
import ru.deceased16.ygmemepad.network.RawPayload;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YGMemePadClient implements ClientModInitializer {

    private static final Logger LOGGER = Logger.getLogger("YGMemePad");

    private static volatile MemeSoundListData lastSoundData = MemeSoundListData.EMPTY;

    private static final AtomicInteger dataVersion = new AtomicInteger(0);

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(RawPayload.ID, RawPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RawPayload.ID, RawPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(RawPayload.ID, (payload, context) ->
                context.client().execute(() -> handleIncoming(payload.data())));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> requestSoundListRefresh());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastSoundData = MemeSoundListData.EMPTY;
            dataVersion.incrementAndGet();
        });

        ClientTickEvents.END_CLIENT_TICK.register(MemePadInput::onTick);
    }

    private static void handleIncoming(byte[] data) {
        try {
            lastSoundData = MemeSoundListDecoder.decode(data);
            dataVersion.incrementAndGet();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Не удалось разобрать список звуков от сервера", e);
        }
    }

    public static void requestSoundListRefresh() {
        if (ClientPlayNetworking.canSend(RawPayload.ID)) {
            ClientPlayNetworking.send(new RawPayload(new byte[0]));
        }
    }

    public static MemeSoundListData getLastSoundData() {
        return lastSoundData;
    }

    public static int getDataVersion() {
        return dataVersion.get();
    }
}
