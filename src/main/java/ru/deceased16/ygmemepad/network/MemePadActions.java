package ru.deceased16.ygmemepad.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import ru.deceased16.ygmemepad.YGMemePadClient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MemePadActions {

    private static final Logger LOGGER = Logger.getLogger("YGMemePad");

    public static final byte OP_REQUEST_LIST = 1;
    public static final byte OP_PLAY = 2;
    public static final byte OP_CREATE_SOUND = 3;
    public static final byte OP_CREATE_CATEGORY = 4;
    public static final byte OP_REMOVE_SOUND = 5;
    public static final byte OP_REMOVE_CATEGORY = 6;

    private static final byte CLIENT_PROTOCOL = 2;

    private MemePadActions() {
    }

    private interface Body {
        void write(DataOutputStream out) throws IOException;
    }

    public static void requestSoundList() {
        send(out -> {
            out.writeByte(OP_REQUEST_LIST);
            out.writeByte(CLIENT_PROTOCOL);
        });
    }

    public static void play(MinecraftClient client, String soundName) {
        if (YGMemePadClient.getLastSoundData().supportsPackets()) {
            send(out -> {
                out.writeByte(OP_PLAY);
                out.writeUTF(soundName);
            });
        } else if (client.player != null) {
            client.player.networkHandler.sendChatCommand("memplay \"" + soundName + "\"");
        }
    }

    public static void createSound(String name, String soundEvent, String category) {
        send(out -> {
            out.writeByte(OP_CREATE_SOUND);
            out.writeUTF(name);
            out.writeUTF(soundEvent);
            out.writeUTF(category == null ? "" : category);
        });
    }

    public static void createCategory(String name) {
        send(out -> {
            out.writeByte(OP_CREATE_CATEGORY);
            out.writeUTF(name);
        });
    }

    public static void removeSound(String name) {
        send(out -> {
            out.writeByte(OP_REMOVE_SOUND);
            out.writeUTF(name);
        });
    }

    public static void removeCategory(String name) {
        send(out -> {
            out.writeByte(OP_REMOVE_CATEGORY);
            out.writeUTF(name);
        });
    }

    private static void send(Body body) {
        if (!ClientPlayNetworking.canSend(RawPayload.ID)) {
            return;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            body.write(out);
            out.flush();
            ClientPlayNetworking.send(new RawPayload(bytes.toByteArray()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Не удалось отправить пакет на сервер", e);
        }
    }
}
