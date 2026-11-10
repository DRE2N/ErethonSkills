package de.erethon.spellbook.spells;

import de.erethon.spellbook.utils.SpellbookFeedback;

import de.erethon.spellbook.Spellbook;
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
        Entity target = caster.getTargetEntity(maxDistance + 1);
        if (target instanceof LivingEntity livingEntity) {
            if (!Spellbook.canAttack(caster, livingEntity)) {
                SpellbookFeedback.send(caster, "<color:#ff0000>Du kannst dieses Ziel nicht angreifen!");
                return false;
            }
            if (target.getLocation().distanceSquared(caster.getLocation()) > maxDistance * maxDistance) {
                SpellbookFeedback.send(caster, "<color:#ff0000>Ziel zu weit entfernt!");
                return false;
            }
            targetEntity = livingEntity;
            return true;
        }
        SpellbookFeedback.send(caster, "<color:#ff0000>Kein gültiges Ziel!");
        return false;
    }
}
