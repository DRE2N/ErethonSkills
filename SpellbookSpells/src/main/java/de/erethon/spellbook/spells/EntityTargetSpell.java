package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public abstract class EntityTargetSpell extends SpellbookSpell {

    protected LivingEntity targetEntity = null;
    private int maxDistance = 32;

    public EntityTargetSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        maxDistance = data.getInt("maxDistance", maxDistance);
    }

    @Override
    public boolean onPrecast() {
        Entity target = caster.getTargetEntity(120);
        if (target instanceof LivingEntity livingEntity) {
            if (target.getLocation().distanceSquared(caster.getLocation()) > maxDistance * maxDistance) {
                caster.sendParsedActionBar("<color:#ff0000>Ziel zu weit entfernt!");
                return false;
            }
            targetEntity = livingEntity;
            return true;
        }
        caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
        return false;
    }
}
