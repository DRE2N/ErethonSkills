package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class Harpoon extends WarriorBaseSpell implements Listener {

    // Launch a spectral trident forward. If it hits an enemy, it deals minor damage and pulls them a short distance towards you.
    // While holding moving backward during hit: The pull is stronger/faster.

    private final double pullStrength = data.getDouble("pullStrength", 1.0);
    private final double pullDistance = data.getDouble("pullDistance", 3.0);
    private final double tridentVelocity = data.getDouble("tridentVelocity", 1.3);
    private final double backwardPullMultiplier = data.getDouble("backwardPullMultiplier", 2.0);

    private Trident trident;

    public Harpoon(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 100; // 5 seconds to allow the trident to hit something
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && !caster.getTags().contains("swordstorm.bladedance");
    }

    @Override
    public boolean onCast() {
        trident = caster.getLocation().getWorld().spawn(caster.getLocation(), Trident.class);
        trident.setVelocity(caster.getLocation().getDirection().multiply(tridentVelocity));
        trident.setDamage(0);
        trident.setGlint(true);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return super.onCast();
    }

    @EventHandler
    private void onTridentHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident hitTrident) || hitTrident != trident) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity hit && Spellbook.canAttack(caster, target) && caster instanceof Player player) {
            double distance = caster.getLocation().distance(hit.getLocation());
            if (distance <= pullDistance) {
                double strength = pullStrength;
                if (player.getCurrentInput().isBackward()) {
                    strength *= backwardPullMultiplier;
                }
                hit.setVelocity(caster.getLocation().toVector().subtract(hit.getLocation().toVector()).normalize().multiply(strength));
            }
            hit.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, hit, true, Attribute.ADVANTAGE_PHYSICAL), PDamageType.PHYSICAL);
        }
        cleanup();
        trident.remove();
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        trident.remove();
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(pullStrength, VALUE_COLOR));
        placeholderNames.add("pullStrength");
        spellAddedPlaceholders.add(Component.text(pullDistance, VALUE_COLOR));
        placeholderNames.add("pullDistance");
        spellAddedPlaceholders.add(Component.text(tridentVelocity, VALUE_COLOR));
        placeholderNames.add("tridentVelocity");
        spellAddedPlaceholders.add(Component.text(backwardPullMultiplier, VALUE_COLOR));
        placeholderNames.add("backwardPullMultiplier");
    }
}
