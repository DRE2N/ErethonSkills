package de.erethon.spellbook.spells.warrior.banners;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LastStandBanner extends WarBanner {

    private EffectData effect = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("BuffLastStand");
    private Set<LivingEntity> alreadyHealed = new HashSet<>();
    private final double healthPercentage = data.getDouble("healthPercentage", 0.1);


    public LastStandBanner(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("keepAliveTicks", 20);
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
            if (entity == caster) continue;
            if (alreadyHealed.contains(entity)) continue;
            if (entity.getHealth() <= entity.getMaxHealth() * healthPercentage) {
                entity.setHealth(entity.getHealth() + Spellbook.getScaledValue(data, caster, entity, Attribute.STAT_HEALINGPOWER));
                entity.addEffect(caster, effect, 20, 1);
                alreadyHealed.add(entity);
            }
        }
    }
}
