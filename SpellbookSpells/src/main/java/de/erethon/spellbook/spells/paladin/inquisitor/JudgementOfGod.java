package de.erethon.spellbook.spells.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.paladin.PaladinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JudgementOfGod extends InquisitorBaseSpell implements Listener {

    // Massive AoE damage spell that deals damage to all enemies in a radius around the caster.
    // Can only be used if the target has maximum judgement stacks.
    // If an enemy dies, it will deal additional damage to all enemies in a smaller radius around the corpse and apply weakness.
    // The initial damage is distributed among all enemies in the radius.

    public int range = data.getInt("range", 24); // Trait: SuppressionOfTheHeretics
    private final int warmupTicks = data.getInt("warmupTicks", 100);
    private final int effectDuration = data.getInt("effectDuration", 200);
    private final int effectStacks = data.getInt("effectStacks", 5);
    public double deathDamageRange = data.getDouble("deathDamageRange", 5); // Trait: SuppressionOfTheHeretics
    public double deathDamageMultiplier = data.getDouble("deathDamageMultiplier", 0.1); // Trait: SuppressionOfTheHeretics
    private final EffectData weakness = Spellbook.getEffectData("Weakness");

    private int ticks = 0;
    private final World world = caster.getWorld();
    private final Set<LivingEntity> targets = new HashSet<>();

    public JudgementOfGod(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = warmupTicks + 100;
    }

    @Override
    protected boolean onPrecast() {
        lookForTarget(range);
        if (target == null) {
            return false;
        }
        int stacks = getJudgementStacksOnTarget(target);
        if (stacks < 7) {
            caster.sendParsedActionBar("<red>Not enough judgement!");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCast() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @Override
    protected void onTick() {
        super.onTick();
        ticks++;
        // Sound the bell every 20 ticks
        if ((ticks < warmupTicks) && (ticks % 20 == 0)) {
            world.playSound(caster, Sound.BLOCK_BELL_USE, 1.0f, 0.7f);
            return;
        }
        // Now count down with bell sounds
        if (ticks == warmupTicks) {
            world.playSound(caster, Sound.BLOCK_BELL_USE, 1.0f, 1.5f);
            for (LivingEntity livingEntity : target.getLocation().getNearbyLivingEntities(range)) {
                if (livingEntity == caster) continue;
                if (!Spellbook.canAttack(caster, livingEntity)) continue;
                rayOfGod(livingEntity, 60, 20);
            }
            return;
        }
        if (ticks == warmupTicks + 10) {
            world.playSound(caster, Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
            for (LivingEntity livingEntity : target.getLocation().getNearbyLivingEntities(range)) {
                if (livingEntity == caster) continue;
                if (!Spellbook.canAttack(caster, livingEntity)) continue;
                rayOfGod(livingEntity, 60, 40);
            }
            return;
        }
        if (ticks == warmupTicks + 20) {
            world.playSound(caster, Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
            for (LivingEntity livingEntity : target.getLocation().getNearbyLivingEntities(range)) {
                if (livingEntity == caster) continue;
                if (!Spellbook.canAttack(caster, livingEntity)) continue;
                rayOfGod(livingEntity, 60, 60);
            }
            return;
        }
        // Judgement time
        if (ticks == warmupTicks + 30) {
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_MAGICAL);
            for (LivingEntity livingEntity : target.getLocation().getNearbyLivingEntities(range)) {
                if (livingEntity == caster) continue;
                if (!Spellbook.canAttack(caster, livingEntity)) continue;
                world.playSound(livingEntity, Sound.ITEM_WOLF_ARMOR_BREAK, 1.0f, 1.0f);
                rayOfGod(livingEntity, 60, 60);
                targets.add(livingEntity);
            }
            triggerTraits(targets);
            double damagePerTarget = damage / targets.size();
            for (LivingEntity livingEntity : targets) {
                livingEntity.damage(damagePerTarget, caster, PDamageType.MAGIC);
                removeJudgement(livingEntity);
            }
        }
    }

    @EventHandler
    private void onDeath(EntityDeathEvent event) {
        if (targets.contains(event.getEntity())) {
            Location deathLocation = event.getEntity().getLocation();
            world.spawnParticle(Particle.EXPLOSION_EMITTER, deathLocation, 1, 0, 0, 0);
            for (LivingEntity livingEntity : event.getEntity().getLocation().getNearbyLivingEntities(deathDamageRange)) {
                if (livingEntity == caster) continue;
                if (!Spellbook.canAttack(caster, livingEntity)) continue;
                rayOfGod(livingEntity, 30, 30);
                livingEntity.addEffect(caster, weakness, effectDuration, effectStacks);
                livingEntity.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, livingEntity, true, Attribute.ADVANTAGE_MAGICAL) * deathDamageMultiplier, caster, PDamageType.MAGIC);
                world.playSound(livingEntity, Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
            }
        }
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        HandlerList.unregisterAll(this);
    }

    // Creates a column of light from above the target. Progress is from the top to the bottom (height).
    private void rayOfGod(LivingEntity target, int height, int progress) {
        int x = target.getLocation().getBlockX();
        int z = target.getLocation().getBlockZ();
        for (int i = height; i > height - progress; i--) {
            double y = target.getLocation().getY() + i;
            world.spawnParticle(Particle.END_ROD, x, y, z, 3, 0, 0.3, 0);
        }
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        placeholderNames.add("duration");
        return super.getPlaceholders(c);
    }
}
