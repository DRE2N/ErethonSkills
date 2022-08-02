package de.erethon.spellbook.spells;

import de.erethon.spellbook.SpellbookSpell;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public abstract class EntityTargetSpell extends SpellbookSpell {

    protected Entity targetEntity = null;
    private int maxDistance = 32;

    public EntityTargetSpell(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
        maxDistance = data.getInt("maxDistance", maxDistance);
    }

    @Override
    public boolean onPrecast() {
        LivingEntity casterEntity = caster.getEntity();
        Entity target = casterEntity.getTargetEntity(maxDistance);
        if (target == null) {
            return false;
        } else {
            targetEntity = target;
            return true;
        }
    }
}
