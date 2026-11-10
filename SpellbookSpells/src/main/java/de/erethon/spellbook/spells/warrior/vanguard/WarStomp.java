package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.combat.BossControl;
import de.erethon.spellbook.combat.CombatSourceContext;
import de.erethon.spellbook.spells.vfx.SkillSignatureVfx;

import de.erethon.spellbook.spells.vfx.TerrainRing;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class WarStomp extends VanguardBaseSpell {

    private static final String SEISMIC_TAG = "vanguard.seismic";

    private final double shockwaveRadius = data.getDouble("shockwaveRadius", 4.0);
    private final double shockwaveHeight = data.getDouble("shockwaveHeight", 3.0);
    private final int playerInterruptTicks = data.getInt("playerInterruptTicks", 4);
    private final int mobStaggerTicks = data.getInt("mobStaggerTicks", 10);
    private final int bossControlPressure = data.getInt("bossControlPressure", 100);

    private final EffectData stunEffectData = Spellbook.getEffectData("Stun");

    private TerrainRing warningRing;

    public WarStomp(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        tickInterval = 0;
        keepAliveTicks = 41;
    }

    @Override
    public boolean onCast() {
        if (!super.onCast()) return false;
        spawnWindupDisplay();
        playWindupEffect();
        return true;
    }

    private void spawnWindupDisplay() {
        warningRing = new TerrainRing(caster.getLocation(), shockwaveRadius,
                "terrain_band_vanguard", 48, 0.42f);
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "lead",
                (float) (shockwaveRadius * 1.7), 20, true);
    }
    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation().add(0, 0.1, 0), 8, 0.5, 0.1, 0.5, 0.05);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.5f);
    }

    private void executeWarStomp() {
        if (warningRing != null) {
            warningRing.close();
            warningRing = null;
        }
        playStompEffect();
        spawnImpactRing();

        for (LivingEntity entity : caster.getWorld().getNearbyLivingEntities(
                caster.getLocation(), shockwaveRadius, shockwaveHeight, shockwaveRadius)) {
            if (entity == caster || !Spellbook.canAttack(caster, entity)) continue;
            double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity,
                    true, Attribute.ADVANTAGE_PHYSICAL);
            double before = entity.getHealth() + entity.getAbsorptionAmount();
            CombatSourceContext.runSpell(this, () -> entity.damage(physicalDamage, caster, PDamageType.PHYSICAL));
            if (entity.getHealth() + entity.getAbsorptionAmount() >= before) continue;
            if (!BossControl.apply(caster, entity, bossControlPressure)) {
                entity.addEffect(caster, stunEffectData,
                        entity instanceof Player ? playerInterruptTicks : mobStaggerTicks, 1);
            }
            entity.getTags().add(SEISMIC_TAG);
            new BukkitRunnable() {
                @Override
                public void run() { entity.getTags().remove(SEISMIC_TAG); }
            }.runTaskLater(Spellbook.getInstance().getImplementer(), 60L);
            playHitEffect(entity);
        }
    }

    private void spawnImpactRing() {
        SkillSignatureVfx.accent(caster.getLocation(), getClass(), "main",
                (float) (shockwaveRadius * 2), 18, true);
    }
    private void playStompEffect() {
        caster.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, caster.getLocation().add(0, 0.1, 0), 30, 1.5, 0.1, 1.5, 0.1,
            Material.STONE.createBlockData());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 0.5, 0),
                5, 0.3, 0.3, 0.3, 0.1,
            Material.STONE.createBlockData());
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 0.8f);
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (warningRing != null) warningRing.move(caster.getLocation());

        if (currentTicks < 20) return;

        if (currentTicks == 20) {
            executeWarStomp();
            return;
        }

        if (currentTicks >= 40) {
            currentTicks = keepAliveTicks;
            return;
        }
    }

    @Override
    protected void cleanup() {
        if (warningRing != null) warningRing.close();
        warningRing = null;
        super.cleanup();
    }
}
