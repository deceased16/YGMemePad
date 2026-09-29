package ru.deceased16.ygmemepad.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RawPayload(byte[] data) implements CustomPayload {

    public static final CustomPayload.Id<RawPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ygutils", "memesounds"));

    public static final PacketCodec<PacketByteBuf, RawPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBytes(value.data()),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new RawPayload(bytes);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
