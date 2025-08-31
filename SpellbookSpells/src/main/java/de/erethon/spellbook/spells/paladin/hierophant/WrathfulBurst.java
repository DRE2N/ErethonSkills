package de.erethon.spellbook.spells.paladin.hierophant;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class WrathfulBurst extends PaladinBaseSpell {

    // The Hierophant unleashes a burst of energy, damaging all enemies in the area.
    // If the Hierophant has more than 50% wrath, the burst deals additional damage and applies a burning effect.
    // Creates expanding rings of fire and light that spread outward from the caster.

    private final double radius = data.getDouble("radius", 5.0);
    private final int minWrathForBurst = data.getInt("minWrathForBurst", 50);
    private final double burstDamageMultiplier = data.getDouble("burstDamageMultiplier", 1.5f);
    private final int burningMinDuration = data.getInt("burningMinDuration", 6) * 20;
    private final int burningMaxDuration = data.getInt("burningMaxDuration", 24) * 20;

    public WrathfulBurst(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && hasEnergy(caster, data); // 25
    }

    @Override
    public boolean onCast() {
        World world = caster.getWorld();
        boolean hasHighWrath = caster.getEnergy() > minWrathForBurst;

        createCircularAoE(caster.getLocation(), radius, 2, 20)
                .onEnter((aoe, entity) -> {
                    if (Spellbook.canAttack(caster, entity)) {
                        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_MAGICAL);
                        if (hasHighWrath) {
                            damage *= burstDamageMultiplier;
                            int burningDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_MAGICAL, burningMinDuration, burningMaxDuration, "burningDuration");
                            entity.addEffect(entity, Spellbook.getEffectData("Burning"), burningDuration, 1);
                            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 8, 0.5, 0.5, 0.5);
                        }
                        entity.damage(damage, caster, PDamageType.MAGIC);
                        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), hasHighWrath ? 8 : 5, 0.5, 0.5, 0.5);
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, hasHighWrath ? 2.0f : 1.2f);
                    }
                })
                .addBlocksOnTopGroundLevel(hasHighWrath ?
                    new Material[]{Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK} :
                    new Material[]{Material.FIRE})
                .sendBlockChanges();

        world.spawnParticle(Particle.EXPLOSION, caster.getLocation().add(0, 1, 0), hasHighWrath ? 3 : 1, 1.0, 0.5, 1.0);
        world.playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, hasHighWrath ? 0.8f : 1.2f);

        if (hasHighWrath) {
            caster.setEnergy(0);
            world.playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.RECORDS, 0.8f, 0.8f);
        }

        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, burningMinDuration, burningMaxDuration, "burningDuration") / 20));
        placeholderNames.add("burningDuration");
    }
}
