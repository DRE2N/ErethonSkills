package de.erethon.spellbook.spells.ranger;

import com.destroystokyo.paper.ParticleBuilder;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.List;

public class BasicSelfHeal extends RangerBaseSpell {

    private final NamespacedKey key = new NamespacedKey("spellbook", "basicselfheal");
    private final int baseHeal = data.getInt("baseHeal", 100);
    public double healingMultiplier = 1.0;
    private AttributeModifier modifier = new AttributeModifier(key, -0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    public BasicSelfHeal(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(modifier);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        new ParticleBuilder(Particle.HEART).allPlayers().location(caster.getLocation().add(0, 1, 0)).count(3).spawn();
    }

    @Override
    protected void onTickFinish() {
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(modifier);
        caster.setHealth(Math.min((caster.getHealth() + Spellbook.getScaledValue(data, caster, caster, Attribute.STAT_HEALINGPOWER) * baseHeal * healingMultiplier), caster.getMaxHealth()));
        triggerTraits();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(Spellbook.getScaledValue(data, caster, caster, Attribute.STAT_HEALINGPOWER) * baseHeal * healingMultiplier, ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("heal amount");
        return super.getPlaceholders(c);
    }
}
