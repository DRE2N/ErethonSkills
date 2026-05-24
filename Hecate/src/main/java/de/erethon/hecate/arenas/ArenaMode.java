package de.erethon.hecate.arenas;

public enum ArenaMode {
    CONTROL,
    ESCORT;

    public static ArenaMode parse(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase()) {
            case "control", "ctf", "points", "capture" -> CONTROL;
            case "escort", "payload" -> ESCORT;
            default -> null;
        };
    }
}
