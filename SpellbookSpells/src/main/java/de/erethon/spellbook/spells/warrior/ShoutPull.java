package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class ShoutPull extends AbstractWarriorShout {

    Entity target = null;

    public int range = data.getInt("range", 10);

    public ShoutPull(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        target = caster.getTargetEntity(range);
        if (target == null || !(target instanceof LivingEntity living) || !Spellbook.canAttack(caster, living)) {
            caster.sendParsedActionBar("<color:#ff0000>Kein gültiges Ziel!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        target.setVelocity(caster.getLocation().getDirection().multiply(-1).multiply(data.getDouble("strength", 1)));
        return super.onCast();
    }
}
