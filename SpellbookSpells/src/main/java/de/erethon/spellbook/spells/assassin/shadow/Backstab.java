package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
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
    // Deals bonus damage if the target is marked and refunds energy if the target dies. Deals bonus damage if the caster is cloaked.
    // Bonus damage scales with advantage_physical.

    private final int energyBonus = data.getInt("energyBonus", 50);
    private final double markedMinDamage = data.getDouble("markedMinDamage", 1.0);
    private final double markedMaxDamage = data.getDouble("markedMaxDamage", 2.0);
    private final double damageMultiplierWhenCloaked = data.getDouble("damageMultiplierWhenCloaked", 1.2);

    private Location location = null;
    private int ticks = 0;
    private boolean teleported = false;

    public Backstab(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
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
        EffectManager manager = Spellbook.getInstance().getEffectManager();
        CylinderEffect effectTarget = new CylinderEffect(manager);
        effectTarget.setLocation(location);
        effectTarget.particle = Particle.DUST;
        effectTarget.particleSize = 0.4f;
        effectTarget.color = Color.BLACK;
        effectTarget.duration = 500;
        effectTarget.height = 1.5f;
        effectTarget.start();
        CylinderEffect effectCaster = new CylinderEffect(manager);
        effectCaster.setEntity(caster);
        effectCaster.particle = Particle.DUST;
        effectCaster.particleSize = 0.4f;
        effectCaster.color = Color.BLACK;
        effectCaster.duration = 500;
        effectCaster.height = 1.0f;
        effectCaster.start();
        keepAliveTicks = 20; // Keep alive for event handling
        ticks = 5; // Teleport delay
        triggerTraits(target, 0);
        return super.onCast();
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        if (event.getEntity().equals(target)) {
            caster.setEnergy(caster.getEnergy() + energyBonus);
        }
    }

    @Override
    protected void onTick() {
        if (ticks > 0 || teleported) {
            ticks--;
            return;
        }
        teleported = true;
        float yaw = target.getLocation().getYaw();
        float pitch = caster.getLocation().getPitch();
        location.setYaw(yaw);
        location.setPitch(pitch);
        caster.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
        caster.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        AttributeModifier bonus;
        double cloakedBonus = 1.0;
        if (caster.getTags().contains("shadow_cloak")) {
            cloakedBonus = damageMultiplierWhenCloaked;
            caster.getTags().remove("shadow_cloak");
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation(), 4, 0.5, 0.5, 0.5, 0.1);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANCIENT_DEBRIS_BREAK, SoundCategory.RECORDS, 0.8f, 1.2f);
        } else {
            caster.getWorld().spawnParticle(Particle.SMOKE, caster.getLocation(), 4, 0.5, 0.5, 0.5, 0.1);
        }
        if (target.getTags().contains("assassin.daggerthrow.marked")) {
            bonus = new AttributeModifier(new NamespacedKey("spellbook", "backstab"),
                    Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL, markedMinDamage, markedMaxDamage, "marked") * cloakedBonus,
                    AttributeModifier.Operation.ADD_NUMBER);
            target.getTags().remove("assassin.daggerthrow.marked");
        } else {
            bonus = new AttributeModifier(new NamespacedKey("spellbook", "backstab"),
                    Spellbook.getScaledValue(data, caster, target, Attribute.ADVANTAGE_PHYSICAL) * cloakedBonus,
                    AttributeModifier.Operation.ADD_NUMBER);
        }
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(bonus);
        caster.attack(target);
        caster.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(bonus);
        triggerTraits(target, 1);
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, markedMinDamage, markedMaxDamage, "marked"), VALUE_COLOR));
        placeholderNames.add("marked");
    }
}
