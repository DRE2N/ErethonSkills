package de.erethon.spellbook.spells.ranger;

import com.destroystokyo.paper.ParticleBuilder;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class BasicSelfHeal extends RangerBaseSpell {

    private final int baseHeal = data.getInt("baseHeal", 100);
    public double healingMultiplier = 1.0;
    private AttributeModifier modifier = new AttributeModifier("BasicSelfHeal", -0.5, AttributeModifier.Operation.ADD_NUMBER);

    public BasicSelfHeal(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 40);
    }

    @Override
    protected boolean onCast() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        new ParticleBuilder(Particle.HEART).allPlayers().location(caster.getLocation().add(0, 1, 0)).count(3).spawn();
    }

    @Override
    protected void onTickFinish() {
        caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        caster.setHealth(Math.min((caster.getHealth() + Spellbook.getScaledValue(data, caster, caster, Attribute.STAT_HEALINGPOWER) * baseHeal * healingMultiplier), caster.getMaxHealth()));
        triggerTraits();
    }
}
