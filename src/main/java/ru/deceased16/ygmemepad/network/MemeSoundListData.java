package ru.deceased16.ygmemepad.network;

import java.util.List;

public record MemeSoundListData(int protocolVersion,
                                int cooldownSeconds,
                                boolean admin,
                                List<String> categories,
                                List<MemeSoundEntry> sounds) implements ServerMessage {

    public static final MemeSoundListData EMPTY = new MemeSoundListData(0, 0, false, List.of(), List.of());

    public boolean supportsPackets() {
        return protocolVersion >= 2;
    }
}
