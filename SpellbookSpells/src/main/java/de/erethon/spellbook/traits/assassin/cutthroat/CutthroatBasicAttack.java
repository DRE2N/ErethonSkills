package de.erethon.spellbook.traits.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class CutthroatBasicAttack extends SpellTrait {

    // Every 3rd attack grants bonus energy and damage. The bonus damage is multiplied by a factor of 1.2.
    // The target is also inflicted with a bleeding effect that lasts between 5 and 10 seconds, scaling with advantage_magical.

    private final int attacksForBonus = data.getInt("attacksForBonus", 3);
    private final int bonusEnergy = data.getInt("bonusEnergy", 5);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.2);
    private final int bleedMinDuration = data.getInt("bleedMinDuration", 5);
    private final int bleedMaxDuration = data.getInt("bleedMaxDuration", 10);

    private int currentAttacks = 0;
    private LivingEntity target;
    private final EffectData bleedEffectData = Spellbook.getEffectData("Bleeding");

    public CutthroatBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (this.target == null) {
            this.target = target;
        }
        if (this.target != target) {
            this.target = target;
            currentAttacks = 0; // Reset the attack count if the target changes
        }
        currentAttacks++;
        if (currentAttacks >= attacksForBonus) {
            currentAttacks = 0;
            caster.addEnergy(bonusEnergy);
            damage *= bonusDamageMultiplier;
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 2.0f);
            caster.getWorld().spawnParticle(Particle.DRIPPING_LAVA, target.getLocation(), 3, 0.5, 0.5, 0.5);
            target.addEffect(caster, bleedEffectData, (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, bleedMinDuration, bleedMaxDuration, "bleed"), 1);
        }
        return super.onAttack(target, damage, type);
    }
}
