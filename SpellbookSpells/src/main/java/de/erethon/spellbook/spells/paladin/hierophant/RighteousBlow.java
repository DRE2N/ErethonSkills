package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class RighteousBlow extends PaladinBaseSpell {

    // RMB: The Hierophant strikes the target with a righteous blow, dealing physical and bonus magical damage. Generates Wrath on hit.
    // If Wrath is above 50, this attack also cleaves on one additional target.

    private final int range = data.getInt("range", 3);
    private final int wrathOnHit = data.getInt("wrathOnHit", 10);
    private final double minimumCleaveWrath = data.getDouble("minimumCleaveWrath", 50);
    private final double cleaveRadius = data.getDouble("cleaveRadius", 3);
    private final double cleaveDamageModifier = data.getDouble("cleaveDamageMultiplier", 0.5);
    private final int maxCleaveTargets = data.getInt("maxCleaveTargets", 1);

    public RighteousBlow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        double magicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);
        target.damage(physicalDamage, caster, PDamageType.PHYSICAL);
        target.damage(magicalDamage, caster, PDamageType.MAGIC);
        caster.setEnergy(caster.getEnergy() + wrathOnHit);
        if (caster.getEnergy() >= minimumCleaveWrath) {
            int cleaveTargets = 0;
            for (LivingEntity entity : target.getLocation().getNearbyLivingEntities(cleaveRadius)) {
                if (entity != target && entity != caster && Spellbook.canAttack(caster, entity) && cleaveTargets < maxCleaveTargets) {
                    double cleaveDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL) * cleaveDamageModifier;
                    entity.damage(cleaveDamage, caster, PDamageType.PHYSICAL);
                    entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation(), 3, 0.5, 0.5, 0.5);
                    cleaveTargets++;
                }
            }
        }
        return super.onCast();
    }
}
