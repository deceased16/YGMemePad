package ru.deceased16.ygmemepad.network;

import java.util.List;

public record MemeSoundListData(int protocolVersion, int cooldownSeconds, List<MemeSoundEntry> sounds) {

    public static final MemeSoundListData EMPTY = new MemeSoundListData(0, 0, List.of());
}
