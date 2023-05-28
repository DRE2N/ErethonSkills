package de.erethon.spellbook.spells.warrior.banners;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class HealingBanner extends WarBanner {

    public HealingBanner(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("keepAliveTicks", 20);
        tickInterval = data.getInt("tickInterval", 10);
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        spawnBanner(caster.getLocation());
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        for (LivingEntity entity : bannerHolder.getLocation().getNearbyLivingEntities(radius)) {
            if (Spellbook.canAttack(caster, entity)) {
                continue;
            }
            entity.setHealth(Math.min(entity.getHealth() + Spellbook.getScaledValue(data, caster, entity, Attribute.STAT_HEALINGPOWER), entity.getMaxHealth()));
        }
    }
}
