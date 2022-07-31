package de.erethon.spellbook;

public enum SpellError {

    COOLDOWN("Nicht bereit"),
    COST("Zu wenig Mana"),
    DISABLED("Deaktiviert"),
    DISTANCE("Zu weit entfernt"),
    INTERNAL_ERROR("Internal error"),
    NO_PVP("PVP deaktiviert"),
    NO_TARGET("Kein Ziel gewählt");

    private final String message;

    SpellError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    }
