package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Backstab extends AssassinBaseSpell implements Listener {

    // Teleports behind the target and deals a powerful attack, granting energy if the target dies within a short time.
    // Deals massive bonus damage if the target is marked and refunds energy if the target dies.
    // Deals additional bonus damage if the caster is cloaked, consuming the cloak effect.
    // Creates shadow rifts at both locations during the teleport. Bonus damage scales with advantage_physical.

    private final int energyBonus = data.getInt("energyBonus", 50);
    private final double markedMinDamage = data.getDouble("markedMinDamage", 1.0);
    private final double markedMaxDamage = data.getDouble("markedMaxDamage", 2.0);
    private final double damageMultiplierWhenCloaked = data.getDouble("damageMultiplierWhenCloaked", 1.5);

    private Location location = null;
    private int ticks = 0;
    private boolean teleported = false;
    private AoE originRift = null;
    private AoE destinationRift = null;

    public Backstab(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 1;
    }

    @Override
    protected boolean onPrecast() {
        if (!lookForTarget()) {
            return false;
        }
        location = target.getLocation().clone().add(0, 1, 0).toVector().subtract(target.getLocation().getDirection().multiply(1.5)).toLocation(target.getWorld());
        if (location.getBlock().isSolid()) {
            caster.sendParsedActionBar("<color:#ff0000>Not enough space behind target");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

        Location startLoc = caster.getLocation().clone();

        caster.getWorld().playSound(startLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.RECORDS, 0.8f, 0.6f);
        caster.getWorld().playSound(startLoc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.RECORDS, 0.6f, 1.8f);
        caster.getWorld().playSound(location, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.RECORDS, 0.7f, 0.8f);

        caster.getWorld().spawnParticle(Particle.DUST, startLoc.add(0, 1, 0), 20, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(Color.BLACK, 1.5f));
        caster.getWorld().spawnParticle(Particle.SMOKE, startLoc.add(0, 0.5, 0), 15, 0.5, 0.5, 0.5, 0.1);

        location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.2f));
        location.getWorld().spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.3);

        originRift = createCircularAoE(startLoc, 1.5, 0.5, 100)
            .addBlockChange(Material.BLACK_CONCRETE)
            .sendBlockChanges();

        destinationRift = createCircularAoE(location, 1.5, 0.5, 100)
            .addBlockChange(Material.CRIMSON_NYLIUM)
            .sendBlockChanges();

        EffectManager manager = Spellbook.getInstance().getEffectManager();
        CylinderEffect effectTarget = new CylinderEffect(manager);
        effectTarget.setLocation(location);
        effectTarget.particle = Particle.DUST;
        effectTarget.particleSize = 0.6f;
        effectTarget.color = Color.fromRGB(139, 0, 0);
        effectTarget.duration = 100;
        effectTarget.height = 2.0f;
        effectTarget.start();

        CylinderEffect effectCaster = new CylinderEffect(manager);
        effectCaster.setEntity(caster);
        effectCaster.particle = Particle.DUST;
        effectCaster.particleSize = 0.5f;
        effectCaster.color = Color.BLACK;
        effectCaster.duration = 100;
        effectCaster.height = 1.5f;
        effectCaster.start();

        keepAliveTicks = 20;
        ticks = 3;
        triggerTraits(target, 0);
        return super.onCast();
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        if (event.getEntity().equals(target)) {
            caster.setEnergy(caster.getEnergy() + energyBonus);

            caster.getWorld().spawnParticle(Particle.ENCHANT, caster.getLocation().add(0, 1, 0), 20, 0.8, 0.8, 0.8, 1);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.RECORDS, 1.0f, 0.8f);
        }
    }

    @Override
    protected void onTick() {
        if (ticks > 0 || teleported) {
            ticks--;
            return;
        }
        teleported = true;

        Location originalLoc = caster.getLocation().clone();
        float yaw = target.getLocation().getYaw();
        float pitch = caster.getLocation().getPitch();
        location.setYaw(yaw);
        location.setPitch(pitch);

        caster.getWorld().spawnParticle(Particle.PORTAL, originalLoc.add(0, 1, 0), 25, 1, 1, 1, 1);
        location.getWorld().spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 25, 1, 1, 1, 1);

        caster.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15, 1));
        caster.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.RECORDS, 1.0f, 1.2f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, SoundCategory.RECORDS, 0.8f, 0.8f);

        AttributeModifier bonus;
        double cloakedBonus = 1.0;

        if (caster.getTags().contains("shadow_cloak")) {
            cloakedBonus = damageMultiplierWhenCloaked;
            caster.getTags().remove("shadow_cloak");
            caster.setInvisible(false);

            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 25, 1, 1, 1, 0.1, new Particle.DustOptions(Color.BLACK, 1.8f));
            caster.getWorld().spawnParticle(Particle.EXPLOSION, caster.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.1);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.RECORDS, 0.6f, 1.5f);
        } else {
            caster.getWorld().spawnParticle(Particle.SMOKE, caster.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.1);
        }

        if (target.getTags().contains("assassin.daggerthrow.marked")) {
            bonus = new AttributeModifier(new NamespacedKey("spellbook", "backstab"),
                    Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, markedMinDamage, markedMaxDamage, "marked") * cloakedBonus,
                    AttributeModifier.Operation.ADD_NUMBER);
            target.getTags().remove("assassin.daggerthrow.marked");

            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.8, 0.8, 0.8, 0.3);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.5f));
        } else {
            bonus = new AttributeModifier(new NamespacedKey("spellbook", "backstab"),
                    Spellbook.getScaledValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL) * cloakedBonus,
                    AttributeModifier.Operation.ADD_NUMBER);
        }

        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(bonus);
        caster.attack(target);
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(bonus);

        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.RECORDS, 1.2f, 0.7f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, SoundCategory.RECORDS, 0.8f, 1.5f);

        triggerTraits(target, 1);
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        if (originRift != null) {
            originRift.revertBlockChanges();
        }
        if (destinationRift != null) {
            destinationRift.revertBlockChanges();
        }
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, markedMinDamage, markedMaxDamage, "marked"), VALUE_COLOR));
        placeholderNames.add("marked");
    }
}
