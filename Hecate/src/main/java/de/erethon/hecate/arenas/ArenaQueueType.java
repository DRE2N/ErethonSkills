package de.erethon.hecate.arenas;

public enum ArenaQueueType {
    RANKED,
    UNRANKED;

    public static ArenaQueueType parse(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase()) {
            case "ranked", "r" -> RANKED;
            case "unranked", "normal", "casual", "u" -> UNRANKED;
            default -> null;
        };
    }
}
