package ru.deceased16.ygmemepad.network;

public record MemeSoundEntry(String name, String category) {

    public boolean hasCategory() {
        return category != null && !category.isEmpty();
    }
}
