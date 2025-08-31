package de.erethon.spellbook.aoe;

/**
 * Interface for spells that want to receive tick callbacks from their AoEs
 */
@FunctionalInterface
public interface AoETickCallback {

    /**
     * Called every tick for an AoE created by this spell
     * @param aoe The AoE that is ticking
     */
    void onAoETick(AoE aoe);
}
