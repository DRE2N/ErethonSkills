package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SealOfDeath extends AssassinBaseSpell implements Listener {

    // Mark a target with a deadly seal (visible debuff). Requires the target to be Marked initially (consumes the Mark).
    // For the next 5 seconds, all damage you deal to the target is increased by 30%.
    // If the target drops below 15% health while the seal is active, the seal detonates, executing the target and
    // creating a 5-block radius cloud of darkness for 3 seconds that obscures vision for all enemies.
    // Debuff scales with advantage_magical.

    private final int range = data.getInt("range", 15);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.3);
    private final double executionThreshold = data.getDouble("executionThreshold", 0.15);
    private final float visionDebuffRadius = (float) data.getDouble("visionDebuffRadius", 5.0);
    private final int visionDebuffDurationMin = data.getInt("visionDebuffDurationMin", 6) * 20;
    private final int visionDebuffDurationMax = data.getInt("visionDebuffDurationMax", 24) * 20;

    private int visualTick = 20;
    private AreaEffectCloud effectCloud;

    public SealOfDeath(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        lookForTarget(range);
        if (target == null || !target.getTags().contains("assassin.daggerthrow.marked")) {
            caster.sendParsedActionBar("<red>Target is not marked!");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        visualTick--;
        if (visualTick <= 0) {
            Location loc = target.getLocation().clone().add(0,1.5,0);
            loc.getWorld().spawnParticle(Particle.WITCH, loc, 10, 1.5, 0.5, 1.5);
        }
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        damage = damage * bonusDamageMultiplier;
        double health = target.getHealth();
        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (health <= maxHealth * executionThreshold) {
            int debuffDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, visionDebuffDurationMin, visionDebuffDurationMax, "debuffDuration");
            target.damage(1000000, caster, PDamageType.PHYSICAL);
            target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation(), 8, 5, 5, 5, 0.1);
            target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
            AreaEffectCloud areaEffectCloud = target.getWorld().spawn(target.getLocation(), AreaEffectCloud.class);
            areaEffectCloud.setDuration(debuffDuration);
            areaEffectCloud.setRadius(visionDebuffRadius);
            areaEffectCloud.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, debuffDuration, 1), true);
            areaEffectCloud.setParticle(Particle.ASH);
            areaEffectCloud.setRadiusPerTick(visionDebuffRadius / debuffDuration);
            effectCloud = areaEffectCloud;
        }
        return super.onAttack(target, damage, type);
    }

    // Only blind enemies in the area effect cloud, not allies
    @EventHandler
    private void onPotionApply(AreaEffectCloudApplyEvent event) {
        if (event.getEntity() == effectCloud) {
            event.getAffectedEntities().removeIf(entity -> entity == caster || !Spellbook.canAttack(caster, entity));
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, visionDebuffDurationMin, visionDebuffDurationMax, "debuffDuration"), VALUE_COLOR));
        placeholderNames.add("debuff");
    }
}
