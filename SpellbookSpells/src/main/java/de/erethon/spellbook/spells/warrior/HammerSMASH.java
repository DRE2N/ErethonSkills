package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.CylinderEffect;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class HammerSMASH extends SpellbookSpell {

    private final EffectManager manager = Spellbook.getInstance().getEffectManager();

    private int animationTicks = 0;
    private final Sound sound = Sound.sound(Key.key("entity.bat.takeoff"), Sound.Source.PLAYER, 0.5f, 0);
    private final double pushVector = data.getDouble("pushVector", 0.3);

    public HammerSMASH(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 30;
    }

    @Override
    protected boolean onCast() {
        return true;
    }

    @Override
    protected void onTick() {
       animationTicks++;
         if (animationTicks == 5) {
             caster.setVelocity(new Vector(0, pushVector, 0).add(caster.getLocation().getDirection()));
         }
         if (animationTicks >= 5 && animationTicks <= 10) {
             caster.playSound(sound);
         }
         if (animationTicks == 15) {
             caster.sendParsedActionBar("<red><bold>SMASH!");
             caster.getWorld().createExplosion(caster.getLocation(), 4, false, false, caster);
             CylinderEffect effect = new CylinderEffect(manager);
             effect.setEntity(caster);
             effect.iterations = 50;
             effect.type = EffectType.REPEATING;
             effect.particle = Particle.REDSTONE;
             effect.particleSize = 1f;
             effect.color = Color.RED;
             effect.duration = 15 * 50;
             effect.height = 0.2f;
             effect.radius = 4f;
             effect.start();
             caster.getLocation().getNearbyEntitiesByType(LivingEntity.class, 4).forEach(entity -> {
                 if (entity == caster) {
                     return;
                 }
                 entity.damage(Spellbook.getVariedAttributeBasedDamage(getData(), caster, entity, false, Attribute.ADV_PHYSICAL), caster, DamageType.PHYSICAL);
             });
         }
    }
}
