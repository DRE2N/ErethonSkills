package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.TerrainRing;
import de.erethon.spellbook.traits.assassin.shadow.ShadowEchoReturnTrait;
import de.erethon.spellbook.utils.SpellbookFeedback;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Eight-second visible return anchor. Enemy hits can destroy it. */
public class ShadowEcho extends ShadowBaseSpell implements Listener {

    private static final NamespacedKey GUARD_KEY = new NamespacedKey("spellbook", "shadow_echo_guard");
    private final TraitData echoTraitData = Bukkit.getServer().getSpellbookAPI().getLibrary()
            .getTraitByID("ShadowEchoReturnTrait");
    private final int maxReturnRange = data.getInt("maxReturnRange", 16);
    private final int guardTicks = data.getInt("guardTicks", 20);
    private ShadowEchoReturnTrait echo;
    private ArmorStand anchor;
    private TerrainRing terrainRing;
    private int anchorHits;

    public ShadowEcho(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        tickInterval = 1;
    }

    @Override
    protected boolean onPrecast() {
        findEchoTrait();
        if (echo != null && echo.isWaitingForReturn()) {
            returnToAnchor();
            return false;
        }
        if (echo != null) {
            caster.removeTrait(echoTraitData);
            echo = null;
        }
        if (!super.onPrecast()) return false;
        caster.addTrait(echoTraitData);
        findEchoTrait();
        return echo != null;
    }

    private void findEchoTrait() {
        for (SpellTrait trait : caster.getActiveTraits()) {
            if (trait instanceof ShadowEchoReturnTrait found) {
                echo = found;
                return;
            }
        }
        echo = null;
    }

    private void returnToAnchor() {
        Location destination = echo.getReturnLocation();
        if (destination == null || destination.getWorld() != caster.getWorld()
                || destination.distanceSquared(caster.getLocation()) > maxReturnRange * maxReturnRange) {
            SpellbookFeedback.send(caster, "<red>Echo anchor is out of range.");
            return;
        }
        if (!destination.getBlock().isPassable()
                || !destination.clone().add(0, 1, 0).getBlock().isPassable()) {
            SpellbookFeedback.send(caster, "<red>Echo anchor is blocked.");
            return;
        }
        Location departure = caster.getLocation().clone();
        if (!caster.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN)) return;
        caster.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.2f);
        SkillSignatureVfx.accent(departure, getClass(), "wake", 2.2f, 16, true);
        SkillSignatureVfx.accent(destination.clone().add(0, 1, 0), getClass(),
                "main", 3.2f, 20, false);
        grantReturnGuard();
        echo.finishReturn();
        caster.removeTrait(echoTraitData);
    }

    private void grantReturnGuard() {
        for (Attribute attribute : new Attribute[]{Attribute.RESISTANCE_PHYSICAL, Attribute.RESISTANCE_MAGICAL}) {
            var instance = caster.getAttribute(attribute);
            if (instance == null) continue;
            if (instance.getModifier(GUARD_KEY) != null) instance.removeModifier(GUARD_KEY);
            double bonus = Math.min(10, 2 + Math.max(0, instance.getValue()) * 0.08);
            instance.addTransientModifier(new AttributeModifier(GUARD_KEY, bonus,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
        Bukkit.getScheduler().runTaskLater(Spellbook.getInstance().getImplementer(), () -> {
            for (Attribute attribute : new Attribute[]{Attribute.RESISTANCE_PHYSICAL, Attribute.RESISTANCE_MAGICAL}) {
                var instance = caster.getAttribute(attribute);
                if (instance != null && instance.getModifier(GUARD_KEY) != null) instance.removeModifier(GUARD_KEY);
            }
        }, guardTicks);
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        Location center = caster.getLocation().clone();
        anchor = center.getWorld().spawn(center, ArmorStand.class, stand -> {
            stand.setSmall(true);
            stand.setVisible(false);
            stand.setGlowing(true);
            stand.setGravity(false);
            stand.setSilent(true);
        });
        echo.setAnchor(center, anchor);
        terrainRing = new TerrainRing(center, 1.35,
                "terrain_band_shadow", 28, 0.32f);
        SkillSignatureVfx.accent(center.clone().add(0, 1, 0), getClass(),
                "main", 2.2f, 22, false);
        caster.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.6f);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (echo == null || !echo.isWaitingForReturn()) {
            currentTicks = keepAliveTicks;
            return;
        }
        if (currentTicks % 10 == 0) {
            anchor.getWorld().spawnParticle(Particle.DUST, anchor.getLocation().add(0, 0.7, 0),
                    4, 0.25, 0.3, 0.25, 0,
                    new Particle.DustOptions(Color.fromRGB(153, 162, 169), 0.9f));
        }
    }

    @EventHandler
    public void onAnchorHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() != anchor || echo == null || !echo.isWaitingForReturn()) return;
        LivingEntity attacker = event.getDamager() instanceof LivingEntity living ? living
                : event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof LivingEntity shooter ? shooter : null;
        event.setCancelled(true);
        if (attacker == null || !Spellbook.canAttack(attacker, caster)) return;
        anchorHits++;
        anchor.getWorld().playSound(anchor.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 0.8f);
        SkillSignatureVfx.accent(anchor.getLocation().add(0, 0.7, 0), getClass(),
                "wake", 1.8f, 12, false);
        if (anchorHits >= 2) {
            echo.finishReturn();
            caster.removeTrait(echoTraitData);
            currentTicks = keepAliveTicks;
        }
    }

    @Override
    protected void cleanup() {
        if (terrainRing != null) terrainRing.close();
        HandlerList.unregisterAll(this);
        if (anchor != null) {
            if (echo != null && echo.isWaitingForReturn()) echo.finishReturn();
            if (echo != null) caster.removeTrait(echoTraitData);
        }
        super.cleanup();
    }
}
