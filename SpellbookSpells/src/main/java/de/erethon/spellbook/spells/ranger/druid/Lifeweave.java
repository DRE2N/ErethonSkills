package de.erethon.spellbook.spells.ranger.druid;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class Lifeweave extends DruidBaseSpell {

    private final double healMin = data.getDouble("healMin", 45);
    private final double healMax = data.getDouble("healMax", 110);
    private final double shieldMin = data.getDouble("shieldMin", 80);
    private final double shieldMax = data.getDouble("shieldMax", 190);
    private final double seedShieldBonus = data.getDouble("seedShieldBonus", 0.3);
    private final int shieldDuration = data.getInt("shieldDuration", 100);
    private final int regenerationDuration = data.getInt("regenerationDuration", 60);
    private final int resistanceDuration = data.getInt("resistanceDuration", 40);

    private final NamespacedKey shieldKey = new NamespacedKey("spellbook", "druid_lifeweave_shield");
    private LivingEntity weaveTarget;
    private AttributeModifier shieldModifier;

    public Lifeweave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = shieldDuration;
    }

    @Override
    protected boolean onPrecast() {
        if (!super.onPrecast()) {
            return false;
        }
        if (lookForAnyLivingTarget(range) && isAlly(target)) {
            weaveTarget = target;
            return true;
        }
        weaveTarget = caster;
        return true;
    }

    @Override
    public boolean onCast() {
        int seeds = consumeSeeds(weaveTarget);
        double heal = Spellbook.getRangedValue(data, caster, weaveTarget, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal");
        double shield = Spellbook.getRangedValue(data, caster, weaveTarget, Attribute.STAT_HEALINGPOWER, shieldMin, shieldMax, "shield") * (1 + seeds * seedShieldBonus);

        weaveTarget.heal(heal);
        applyRegeneration(weaveTarget, regenerationDuration + seeds * 20, 1);
        if (resistance != null && seeds > 0) {
            weaveTarget.addEffect(caster, resistance, resistanceDuration + seeds * 10, 1);
        }

        AttributeInstance maxHealth = weaveTarget.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && shield > 0) {
            shieldModifier = new AttributeModifier(shieldKey, shield, AttributeModifier.Operation.ADD_NUMBER);
            if (maxHealth.getModifier(shieldKey) != null) {
                return false;
            }
            maxHealth.addTransientModifier(shieldModifier);
            weaveTarget.heal(shield);
        }

        weaveTarget.getWorld().spawnParticle(Particle.HEART, weaveTarget.getLocation().add(0, 1.2, 0), 4 + seeds, 0.35, 0.45, 0.35, 0);
        weaveTarget.getWorld().spawnParticle(Particle.DUST, weaveTarget.getLocation().add(0, 1, 0), 14, 0.45, 0.55, 0.45, 0,
            new Particle.DustOptions(DRUID_GOLD, 1.2f));
        weaveTarget.getWorld().playSound(weaveTarget.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.RECORDS, 0.8f, 1.6f);
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        if (weaveTarget != null && shieldModifier != null) {
            AttributeInstance maxHealth = weaveTarget.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.removeModifier(shieldModifier);
            }
            shieldModifier = null;
        }
        super.cleanup();
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, healMin, healMax, "heal"), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("heal");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.STAT_HEALINGPOWER, shieldMin, shieldMax, "shield"), ATTR_HEALING_POWER_COLOR));
        placeholderNames.add("shield");
    }
}
