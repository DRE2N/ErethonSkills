package de.erethon.spellbook.spells.assassin;

import com.destroystokyo.paper.ParticleBuilder;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class Assassination extends SpellbookSpell {

    AttributeModifier critModifier;
    AttributeModifier critChanceModifier;
    AttributeModifier resistanceModifier;

    public Assassination(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 10) * 20;
    }

    @Override
    protected boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        critModifier = new AttributeModifier("assasinationCrit-" + caster.getUniqueId(), data.getInt("critAmount", 20), AttributeModifier.Operation.ADD_NUMBER);
        critChanceModifier = new AttributeModifier("assasinationChance-" + caster.getUniqueId(), data.getInt("critChance", 50), AttributeModifier.Operation.ADD_NUMBER);
        resistanceModifier = new AttributeModifier("assasinationRes-" + caster.getUniqueId(), data.getInt("resistances", -20), AttributeModifier.Operation.ADD_NUMBER);
        caster.getAttribute(Attribute.STAT_CRIT_DMG).addModifier(critModifier);
        caster.getAttribute(Attribute.STAT_CRIT_CHANCE).addModifier(critChanceModifier);
        caster.getAttribute(Attribute.RES_AIR).addModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_EARTH).addModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_FIRE).addModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_WATER).addModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_FIRE).addModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_PHYSICAL).addModifier(resistanceModifier);
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 30));
    }

    @Override
    protected void onTick() {
        for (int i = 0; i < 5; i++) {
            new ParticleBuilder(Particle.REDSTONE).color(255, 0,50).allPlayers().location(caster.getLocation().add(0, -1, 0)).spawn();
        }
    }

    @Override
    protected void cleanup() {
        caster.getAttribute(Attribute.STAT_CRIT_DMG).removeModifier(critModifier);
        caster.getAttribute(Attribute.STAT_CRIT_CHANCE).removeModifier(critChanceModifier);
        caster.getAttribute(Attribute.RES_AIR).removeModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_EARTH).removeModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_FIRE).removeModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_WATER).removeModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_FIRE).removeModifier(resistanceModifier);
        caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(resistanceModifier);
    }
}

