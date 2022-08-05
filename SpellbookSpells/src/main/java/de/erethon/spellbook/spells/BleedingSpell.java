package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.entity.LivingEntity;

public class BleedingSpell extends EntityTargetSpell {

    public BleedingSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        targetEntity.getWorld().playEffect(targetEntity.getLocation(), Effect.GHAST_SHRIEK, 1);
        keepAliveTicks = 100;
        tickInterval = 6;
        return true;
    }

    @Override
    public void onAfterCast() {
        caster.setCooldown(data);
    }

    @Override
    public void onTick() {
        if (targetEntity == null) {
            return;
        }
        if (targetEntity instanceof LivingEntity livingEntity) {
            livingEntity.customName(Component.text(livingEntity.getHealth()));
            if (livingEntity.isDead() ||livingEntity.getHealth() <= 0) {
                return;
            }
            livingEntity.setHealth(livingEntity.getHealth() - 2);
            livingEntity.playEffect(EntityEffect.HURT);
        }
    }

    @Override
    protected void onTickFinish() {
    }
}

