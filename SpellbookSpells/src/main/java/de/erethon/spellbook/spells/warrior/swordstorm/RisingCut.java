package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class RisingCut extends SwordstormBaseSpell implements Listener {

    // A swift upward slash that deals moderate damage. If Jump key is pressed immediately after connecting the hit,
    // the target is launched into the air, interrupting running spells, and the Swordstorm can attack again, dealing increased damage.

    private final int range = data.getInt("range", 5);
    private final int ticksForFollowUp = data.getInt("ticksForFollowUp", 20);
    private final double upwardLaunchVelocityMin = data.getDouble("upwardLaunchVelocityMin", 0.5);
    private final double upwardLaunchVelocityMax = data.getDouble("upwardLaunchVelocityMax", 1.0);
    private final double followUpDamageMultiplierMin = data.getDouble("followUpDamageMultiplierMin", 1.5);
    private final double followUpDamageMultiplierMax = data.getDouble("followUpDamageMultiplierMax", 2.0);

    private boolean wasLaunched = false;
    private ItemDisplay launchMark;

    public RisingCut(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = ticksForFollowUp;
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget(range) && !caster.getTags().contains("swordstorm.bladedance");
    }

    @Override
    public boolean onCast() {
        caster.swingMainHand();
        playRisingBlade(target.getLocation());
        double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL);
        target.damage(damage, caster, PDamageType.PHYSICAL);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        World world = caster.getWorld();
        world.playSound(target.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, SoundCategory.RECORDS, 1, 0.5f);
        world.spawnParticle(Particle.SMALL_GUST, target.getLocation(), 3);
        return super.onCast();
    }

    @Override
    public double onAttack(LivingEntity t, double damage, PDamageType type) {
        if (wasLaunched && t == target) {
            double damageMultiplier = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, followUpDamageMultiplierMin, followUpDamageMultiplierMax, "followUpDamageMultiplier");
            damage *= damageMultiplier;
            World world = caster.getWorld();
            world.playSound(t.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, SoundCategory.RECORDS, 1, 1);
            world.spawnParticle(Particle.CLOUD, target.getLocation(), 3);
            playFollowUpBreak(t);
        }
        return super.onAttack(t, damage, type);
    }

    @EventHandler
    private void onInputEvent(PlayerInputEvent event) {
        if (event.getPlayer() != caster || wasLaunched) {
            return;
        }
        if (event.getInput().isJump() && target != null && target.isValid()) {
            double upwardLaunchVelocity = Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, upwardLaunchVelocityMin, upwardLaunchVelocityMax, "upwardLaunchVelocity");
            Vector launchVelocity = new Vector(0, upwardLaunchVelocity, 0);
            target.setVelocity(target.getVelocity().add(launchVelocity));
            playLaunchCage(target);
            World world = caster.getWorld();
            world.playSound(target.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, SoundCategory.RECORDS, 1, 1);
            Location location = target.getLocation().clone().add(0, -2, 0);
            world.spawnParticle(Particle.GUST, location, 8, 0.5, 0.5, 0.5);
            HandlerList.unregisterAll(this);
            wasLaunched = true;
            for (SpellbookSpell spell : target.getActiveSpells()) {
                spell.interrupt();
            }
        }
    }

    private void playRisingBlade(Location targetLocation) {
        SkillSignatureVfx.accent(targetLocation.clone().add(0, 1, 0), getClass(),
                "main", 2.2f, 8, false);
    }

    private void playLaunchCage(LivingEntity entity) {
        if (launchMark != null && launchMark.isValid()) launchMark.remove();
        launchMark = trackVfx(SkillVfx.billboard(entity.getLocation().add(0, 2.2, 0),
                "sig_warrior_swordstorm_risingcut_wake_f4", 1.5f));
        trackVfxTask(new org.bukkit.scheduler.BukkitRunnable() {
            private int age;
            @Override public void run() {
                if (++age >= ticksForFollowUp || !entity.isValid() || launchMark == null
                        || !launchMark.isValid()) {
                    if (launchMark != null && launchMark.isValid()) launchMark.remove();
                    cancel();
                    return;
                }
                launchMark.teleport(entity.getLocation().add(0, 2.2, 0));
            }
        }.runTaskTimer(Spellbook.getInstance().getImplementer(), 1L, 1L));
    }

    private void playFollowUpBreak(LivingEntity entity) {
        if (launchMark != null && launchMark.isValid()) launchMark.remove();
        SkillSignatureVfx.accent(entity.getLocation().add(0, 1.5, 0), getClass(),
                "main", 2.5f, 10, false);
    }
    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, upwardLaunchVelocityMin, upwardLaunchVelocityMax, "upwardLaunchVelocity"), VALUE_COLOR));
        placeholderNames.add("upwardLaunchVelocity");
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_MAGICAL, followUpDamageMultiplierMin, followUpDamageMultiplierMax, "followUpDamageMultiplier"), VALUE_COLOR));
        placeholderNames.add("followUpDamageMultiplier");
    }
}
