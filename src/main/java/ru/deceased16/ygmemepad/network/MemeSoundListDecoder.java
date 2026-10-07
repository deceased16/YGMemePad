package ru.deceased16.ygmemepad.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MemeSoundListDecoder {

    private static final int S2C_LIST = 1;
    private static final int S2C_RESULT = 2;
    private static final int MAX_ITEMS = 100_000;

    private MemeSoundListDecoder() {
    }

    public static ServerMessage decode(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int version = in.readByte();

            if (version <= 1) {
                int cooldownSeconds = in.readInt();
                List<MemeSoundEntry> sounds = readSounds(in);
                return new MemeSoundListData(version, cooldownSeconds, false, deriveCategories(sounds), sounds);
            }

            int type = in.readByte();
            return switch (type) {
                case S2C_LIST -> readListV2(in, version);
                case S2C_RESULT -> {
                    int opcode = in.readByte();
                    boolean success = in.readBoolean();
                    String message = in.readUTF();
                    yield new ActionResult(opcode, success, message);
                }
                default -> throw new IOException("Неизвестный тип сообщения: " + type);
            };
        }
    }

    private static MemeSoundListData readListV2(DataInputStream in, int version) throws IOException {
        int cooldownSeconds = in.readInt();
        boolean admin = in.readBoolean();

        int categoryCount = checkCount(in.readInt());
        List<String> categories = new ArrayList<>(categoryCount);
        for (int i = 0; i < categoryCount; i++) {
            categories.add(in.readUTF());
        }

        List<MemeSoundEntry> sounds = readSounds(in);
        return new MemeSoundListData(version, cooldownSeconds, admin, categories, sounds);
    }

    private static List<MemeSoundEntry> readSounds(DataInputStream in) throws IOException {
        int count = checkCount(in.readInt());
        List<MemeSoundEntry> sounds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = in.readUTF();
            String category = in.readUTF();
            sounds.add(new MemeSoundEntry(name, category));
        }
        return sounds;
    }

    private static int checkCount(int count) throws IOException {
        if (count < 0 || count > MAX_ITEMS) {
            throw new IOException("Некорректное количество элементов: " + count);
        }
        return count;
    }

    private static List<String> deriveCategories(List<MemeSoundEntry> sounds) {
        return sounds.stream()
                .filter(MemeSoundEntry::hasCategory)
                .map(MemeSoundEntry::category)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
