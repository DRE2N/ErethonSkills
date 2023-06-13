package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ProjectileRelatedSkill extends RangerBaseSpell implements Listener {

    protected boolean shouldEndWhenProjectileHits = true;
    protected boolean hasTrail = true;
    protected Color trailColor = Color.RED;
    protected int affectedArrows = 1;
    private ParticleEffect effect = new ParticleEffect(Spellbook.getInstance().getEffectManager());
    private int arrowsShot = 0;
    private int arrowsHit = 0;

    public ProjectileRelatedSkill(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        keepAliveTicks = 200; // 10 Seconds should be enough to shoot an arrow
    }

    @EventHandler
    public void onShootEvent(EntityShootBowEvent event) {
        if (event.getEntity() != caster) return;
        if (hasTrail && (arrowsShot <= affectedArrows)) addEffect((Projectile) event.getProjectile());
        arrowsShot++;
        onShoot(event);
    }

    protected void onShoot(EntityShootBowEvent event) {
    }

    @EventHandler
    public void onHitEvent(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() != caster) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!Spellbook.canAttack(living, caster)) return;
        onHit(event);
        if (shouldEndWhenProjectileHits && (arrowsHit >= affectedArrows)) cleanup();
        arrowsHit++;
    }

    protected void onHit(ProjectileHitEvent event) {
    }

    @EventHandler
    public void onDamageEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile)) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (projectile.getShooter() != caster) return;
        if (!Spellbook.canAttack(living, caster)) return;
        onDamage(event, projectile);
    }

    protected void onDamage(EntityDamageByEntityEvent event, Projectile projectile) {
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
        effect.cancel();
    }

    protected void addEffect(Projectile projectile) {
        effect.setEntity(projectile);
        effect.disappearWithOriginEntity = true;
        effect.duration = 20000;
        effect.iterations = -1;
        effect.particle = Particle.REDSTONE.builder().data(new Particle.DustOptions(org.bukkit.Color.RED, 1)).particle();
        effect.color = trailColor;
        effect.start();
    }
}
