package de.erethon.spellbook.spells;

import de.erethon.spellbook.ActiveSpell;
import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import net.kyori.adventure.text.Component;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class BleedingSpell extends SpellData {

    public BleedingSpell(Spellbook spellbook, String id) {
        super(spellbook, id);
    }

    @Override
    public boolean precast(SpellCaster caster, ActiveSpell activeSpell) {
        LivingEntity casterEntity = caster.getEntity();
        Entity target = casterEntity.getTargetEntity(32);
        if (target == null) {
            return false;
        } else {
            activeSpell.setTargetEntity(target);
            return true;
        }
    }

    @Override
    public boolean cast(SpellCaster caster, ActiveSpell activeSpell) {
        Entity target = activeSpell.getTargetEntity();
        target.getWorld().playEffect(target.getLocation(), Effect.GHAST_SHRIEK, 1);
        activeSpell.setKeepAliveTicks(100);
        activeSpell.setTickInterval(10);
        return true;
    }

    @Override
    public void afterCast(SpellCaster caster, ActiveSpell activeSpell) {
        caster.setCooldown(this);
    }

    @Override
    public void tick(SpellCaster caster, ActiveSpell activeSpell) {
        Entity target = activeSpell.getTargetEntity();
        if (target == null) {
            return;
        }
        if (target instanceof LivingEntity livingEntity) {
            livingEntity.customName(Component.text(livingEntity.getHealth()));
            if (livingEntity.isDead() ||livingEntity.getHealth() <= 0) {
                return;
            }
            livingEntity.setHealth(livingEntity.getHealth() - 2);
            livingEntity.playEffect(EntityEffect.HURT);
        }
    }
}

