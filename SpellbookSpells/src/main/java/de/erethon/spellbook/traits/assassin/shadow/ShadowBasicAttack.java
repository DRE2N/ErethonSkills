package de.erethon.spellbook.traits.assassin.shadow;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.util.Vector;

public class ShadowBasicAttack extends SpellTrait implements Listener {

    // Basic attack trait for the Shadow class. Grants bonus energy and damage when attacking from behind.
    // Additionally, the shadow gains a double jump ability.

    private final double backAngleForBonus = data.getInt("backAngleForBonus", 40);
    private final int bonusEnergy = data.getInt("bonusEnergy", 5);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.2);
    private final double doubleJumpMultiplier = data.getDouble("doubleJumpMultiplier", 0.4);
    private final double doubleJumpVelocity = data.getDouble("doubleJumpVelocity", 0.8);

    private int lastJumpTick = 0;

    public ShadowBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        HandlerList.unregisterAll(this);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        double angle = Math.abs(caster.getLocation().getYaw() - target.getLocation().getYaw());
        // If the caster is not behind the target, just return the damage
        if (angle > backAngleForBonus) {
            return super.onAttack(target, damage, type);
        }
        caster.addEnergy(bonusEnergy);
        damage *= bonusDamageMultiplier;
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 2.0f);
        caster.getWorld().spawnParticle(Particle.DRIPPING_LAVA, target.getLocation(), 3, 0.5, 0.5, 0.5);
        return damage;
    }

    @EventHandler
    private void onJump(PlayerInputEvent event) {
        if (event.getPlayer() != caster) {
            return;
        }
        if (caster.isOnGround()) {
            lastJumpTick = 0;
            return;
        }
        if (event.getPlayer().isSneaking() || !event.getInput().isJump()) {
            return;
        }
        if (lastJumpTick == 0) {
            Vector vector = caster.getEyeLocation().getDirection().normalize().multiply(doubleJumpMultiplier);
            vector.setY(doubleJumpVelocity);
            caster.setVelocity(caster.getVelocity().add(vector));
            caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation(), 3, 0.5, 0.5, 0.5, 0);
            lastJumpTick = Bukkit.getCurrentTick();
        }
    }
}
