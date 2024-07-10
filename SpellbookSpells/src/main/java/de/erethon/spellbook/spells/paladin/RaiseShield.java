package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

public class RaiseShield extends PaladinBaseSpell {

    private final NamespacedKey key = new NamespacedKey("spellbook", "raiseshield");
    private final AttributeModifier modifier = new AttributeModifier(key, Spellbook.getScaledValue(data, caster, Attribute.RES_MAGIC), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    private CircleEffect circle;

    public RaiseShield(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("keepAliveTicks", 200);
    }

    @Override
    public boolean onCast() {
        caster.getAttribute(Attribute.RES_MAGIC).addTransientModifier(modifier);
        caster.getAttribute(Attribute.RES_PHYSICAL).addTransientModifier(modifier);
        triggerTraits(0);
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, Sound.Source.RECORD, 1, 2));
        circle = new CircleEffect(Spellbook.getInstance().getEffectManager());
        circle.radius = 1.2f;
        circle.particle = Particle.SOUL_FIRE_FLAME;
        circle.particleCount = 8;
        circle.duration = keepAliveTicks * 50;
        circle.iterations = -1;
        circle.setEntity(caster);
        circle.start();
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        caster.getAttribute(Attribute.RES_MAGIC).removeModifier(modifier);
        caster.getAttribute(Attribute.RES_PHYSICAL).removeModifier(modifier);
        triggerTraits(1);
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, Sound.Source.RECORD, 1, 2));
        circle.cancel();
    }
}
