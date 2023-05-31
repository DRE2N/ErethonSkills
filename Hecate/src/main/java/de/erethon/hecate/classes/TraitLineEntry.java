package de.erethon.hecate.classes;

import de.erethon.spellbook.api.TraitData;

public record TraitLineEntry(TraitData data, int level, int cost, boolean combatOnly) {
}
