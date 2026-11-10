package de.erethon.spellbook.spells.warrior.swordstorm;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;
import de.erethon.spellbook.spells.vfx.SkillVfx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Short fishing-rod tap that wins spacing without a long forced pull. */
public class Harpoon extends SwordstormBaseSpell implements Listener {

    private final double range = data.getDouble("range", 8);
    private final double projectileSpeed = data.getDouble("projectileSpeed", 1.5);
    private Snowball hook;
    private final List<ItemDisplay> line = new ArrayList<>();

    public Harpoon(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 20;
        tickInterval = 1;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        hook = caster.getWorld().spawn(caster.getEyeLocation(), Snowball.class);
        hook.setShooter(caster);
        hook.setVelocity(caster.getEyeLocation().getDirection().multiply(projectileSpeed));
        for (int index = 0; index < 4; index++) {
            line.add(trackVfx(SkillVfx.projectile(caster.getEyeLocation(), hook.getVelocity(),
                    "sig_warrior_swordstorm_harpoon_lead_f4", 0.36f)));
        }
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 0.85f, 1.25f);
        return true;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (hook == null || !hook.isValid()
                || hook.getLocation().distanceSquared(caster.getLocation()) > range * range) {
            currentTicks = keepAliveTicks;
            return;
        }
        Location start = caster.getEyeLocation();
        Vector span = hook.getLocation().toVector().subtract(start.toVector());
        for (int index = 0; index < line.size(); index++) {
            double fraction = (index + 1.0) / (line.size() + 1.0);
            ItemDisplay segment = line.get(index);
            if (segment.isValid()) {
                segment.teleport(start.clone().add(span.clone().multiply(fraction)));
                SkillVfx.orientProjectile(segment, span);
            }
        }
    }

    @EventHandler
    public void onHookHit(ProjectileHitEvent event) {
        if (event.getEntity() != hook) return;
        if (event.getHitEntity() instanceof LivingEntity target && Spellbook.canAttack(caster, target)) {
            double damage = Spellbook.getVariedAttributeBasedDamage(data, caster, target,
                    true, Attribute.ADVANTAGE_PHYSICAL);
            double before = target.getHealth() + target.getAbsorptionAmount();
            CombatSourceContext.runSpell(this, () -> target.damage(damage, caster, PDamageType.PHYSICAL));
            if (target.getHealth() + target.getAbsorptionAmount() < before) {
                if (!BossControl.apply(caster, target, 15) && target instanceof Player player) {
                    player.setSprinting(false);
                    Vector velocity = player.getVelocity();
                    player.setVelocity(velocity.multiply(0.8));
                }
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE,
                        0.9f, 0.9f);
                SkillSignatureVfx.accent(target.getLocation().add(0, 1.1, 0),
                        getClass(), "wake", 0.8f, 6, false);
            }
        }
        currentTicks = keepAliveTicks;
        hook.remove();
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        if (hook != null && hook.isValid()) hook.remove();
        super.cleanup();
    }
}
