package ru.deceased16.ygmemepad.network;

public record ActionResult(int opcode, boolean success, String message) implements ServerMessage {
}
