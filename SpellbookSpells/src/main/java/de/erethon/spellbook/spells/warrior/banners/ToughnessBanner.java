package de.erethon.spellbook.spells.warrior.banners;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class ToughnessBanner extends WarBanner {

    Collection<LivingEntity> affected = new HashSet<>();

    public ToughnessBanner(LivingEntity caster, SpellData spellData) {
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
        for (LivingEntity affected : affected) {
            affected.getAttribute(Attribute.RES_PHYSICAL).getModifiers().forEach(modifier -> {
                if (modifier.getName().equals("ToughnessBanner")) {
                    affected.getAttribute(Attribute.RES_PHYSICAL).removeModifier(modifier);
                }
            });
        }
        affected = bannerHolder.getLocation().getNearbyLivingEntities(radius);
        for (LivingEntity entity : affected) {
            if (affected == caster) {
                continue;
            }
            entity.getAttribute(Attribute.RES_PHYSICAL).addModifier(new AttributeModifier("ToughnessBanner", Spellbook.getScaledValue(data, caster, entity, Attribute.RES_PHYSICAL), AttributeModifier.Operation.ADD_NUMBER));
        }
    }
}
