package de.erethon.spellbook.spells.assassin.saboteur;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class SerratedBolt extends AssassinBaseSpell implements Listener {

    private final NamespacedKey markerKey = new NamespacedKey(Spellbook.getInstance().getImplementer(), "serrated_bolt_marker");

    private final EffectData bleedEffectData = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Bleeding");
    private final int bleedDurationTicks = data.getInt("bleedDuration", 6) * 20;
    private final int bleedStacks = data.getInt("bleedStacks", 1);
    private final double projectileSpeed = data.getDouble("projectileSpeed", 2.0);

    public SerratedBolt(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 100; // 5 seconds should be enough to hit an arrow
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) {
            return false;
        }
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

        Location eyeLocation = caster.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        Arrow arrow = caster.launchProjectile(Arrow.class, direction.multiply(projectileSpeed));
        arrow.setShooter(caster);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);

        eyeLocation.getWorld().playSound(eyeLocation, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        eyeLocation.getWorld().playSound(eyeLocation, Sound.ITEM_CROSSBOW_SHOOT, 0.8f, 1.2f);
        triggerTraits();
        return true;
    }

    @EventHandler
    private void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() == null) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) return;
        if (arrow.getShooter() != caster) return;
        if (!(event.getHitEntity() instanceof LivingEntity hit)) return;
        hit.addEffect(caster, bleedEffectData, bleedDurationTicks, bleedStacks);
        hit.getWorld().playSound(hit.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);
        hit.getWorld().spawnParticle(Particle.SWEEP_ATTACK, hit.getLocation(), 2, 0.5, 0.5, 0.5, 0.1);
        hit.getWorld().spawnParticle(Particle.DUST, hit.getLocation(), 8, 0.5, 0.5, 0.5, 0.1);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }
}