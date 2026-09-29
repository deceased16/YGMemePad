package ru.deceased16.ygmemepad.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MemeSoundListDecoder {

    private MemeSoundListDecoder() {
    }

    public static MemeSoundListData decode(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int protocolVersion = in.readByte();
            int cooldownSeconds = in.readInt();
            int count = in.readInt();

            List<MemeSoundEntry> sounds = new ArrayList<>(Math.max(0, count));
            for (int i = 0; i < count; i++) {
                String name = in.readUTF();
                String category = in.readUTF();
                sounds.add(new MemeSoundEntry(name, category));
            }

            return new MemeSoundListData(protocolVersion, cooldownSeconds, sounds);
        }
    }
}
