package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class ShoutFearMe extends AbstractWarriorShout {

    private EffectData fearEffect = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Fear");
    public int duration = data.getInt("duration", 10);

    public ShoutFearMe(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Location inFront = caster.getLocation().add(caster.getLocation().getDirection().multiply(2));
        for (LivingEntity entity : inFront.getWorld().getNearbyEntitiesByType(LivingEntity.class, inFront, range)) {
            if (!Spellbook.canAttack(caster, entity)) {
                continue;
            }
            entity.addEffect(caster, fearEffect, duration, 1);
        }
        return super.onCast();
    }
}
