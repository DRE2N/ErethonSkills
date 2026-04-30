package de.erethon.spellbook.spells.ranger.druid;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class VerdantBloom extends DruidBaseSpell {

    private final double healMin = data.getDouble("healMin", 40);
    private final double healMax = data.getDouble("healMax", 120);
    private final double splashHealFactor = data.getDouble("splashHealFactor", 0.5);
    private final double seedHealBonus = data.getDouble("seedHealBonus", 0.25);
    private final double seedDamageBonus = data.getDouble("seedDamageBonus", 0.3);
    private final double damageMin = data.getDouble("damageMin", 20);
    private final double damageMax = data.getDouble("damageMax", 60);
    private final double radius = data.getDouble("radius", 4.5);
    private final int regenerationDuration = data.getInt("regenerationDuration", 60);
    private final int slowDuration = data.getInt("slowDuration", 40);
    private final int slowStacks = data.getInt("slowStacks", 1);

    public VerdantBloom(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast()) {
            return false;
        }
        if (!lookForAnyLivingTarget(range)) {
            target = caster;
        }
        return true;
    }

    @Override
    public boolean onCast() {
        LivingEntity bloomTarget = target == null ? caster : target;
        int targetSeeds = consumeSeeds(bloomTarget);
        double primaryHeal = Spellbook.getRangedValue(data, caster, bloomTarget, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal");
        double splashHeal = primaryHeal * splashHealFactor;
        double damage = Spellbook.getRangedValue(data, caster, bloomTarget, Attribute.ADVANTAGE_MAGICAL, damageMin, damageMax, "damage");

        if (isEnemy(bloomTarget)) {
            bloomTarget.damage(damage * (1 + targetSeeds * seedDamageBonus), caster);
            applySlow(bloomTarget, slowDuration + targetSeeds * 15, slowStacks);
            if (targetSeeds == 0) {
                addSeed(bloomTarget, 1);
            }
        } else {
            bloomTarget.heal(primaryHeal * (1 + targetSeeds * seedHealBonus));
            applyRegeneration(bloomTarget, regenerationDuration + targetSeeds * 15, 1);
            if (targetSeeds == 0) {
                addSeed(bloomTarget, 1);
            }
        }

        bloomTarget.getWorld().playSound(bloomTarget.getLocation(), Sound.BLOCK_MOSS_BREAK, SoundCategory.RECORDS, 1.0f, 0.8f);
        bloomTarget.getWorld().playSound(bloomTarget.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.RECORDS, 0.7f, 1.4f);
        bloomTarget.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, bloomTarget.getLocation().add(0, 1, 0), 12, 0.5, 0.6, 0.5, 0.02);
        bloomTarget.getWorld().spawnParticle(Particle.DUST, bloomTarget.getLocation().add(0, 1, 0), 18, 0.8, 0.3, 0.8, 0,
            new Particle.DustOptions(Color.fromRGB(105, 186, 87), 1.4f));

        for (LivingEntity nearby : bloomTarget.getLocation().getNearbyLivingEntities(radius)) {
            if (nearby.equals(bloomTarget)) {
                continue;
            }
            if (Spellbook.canAttack(caster, nearby)) {
                int seeds = consumeSeeds(nearby);
                nearby.damage(damage * (1 + seeds * seedDamageBonus), caster);
                applySlow(nearby, slowDuration + seeds * 10, slowStacks);
                if (seeds == 0) {
                    addSeed(nearby, 1);
                }
                nearby.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, nearby.getLocation().add(0, 1, 0), 6, 0.4, 0.6, 0.4, 0.02);
                continue;
            }
            int seeds = consumeSeeds(nearby);
            nearby.heal(splashHeal * (1 + seeds * seedHealBonus));
            applyRegeneration(nearby, regenerationDuration / 2 + seeds * 10, 1);
            if (seeds == 0) {
                addSeed(nearby, 1);
            }
            nearby.getWorld().spawnParticle(Particle.HEART, nearby.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0);
        }

        return super.onCast();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal"), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("heal");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, damageMin, damageMax, "damage"), ATTR_MAGIC_COLOR));
        placeholderNames.add("damage");
    }
}
