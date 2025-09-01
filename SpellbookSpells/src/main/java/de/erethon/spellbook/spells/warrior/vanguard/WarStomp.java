package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class WarStomp extends WarriorBaseSpell {

    // Slam your foot down, creating a shockwave that damages and briefly stuns

    private final double shockwaveRadius = data.getDouble("shockwaveRadius", 4.0);
    private final double shockwaveHeight = data.getDouble("shockwaveHeight", 3.0);
    private final int minStunDuration = data.getInt("minStunDuration", 2) * 20;
    private final int maxStunDuration = data.getInt("maxStunDuration", 5) * 20;

    private final EffectData stunEffectData = Spellbook.getEffectData("Stun");

    public WarStomp(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        playWindupEffect();
        return super.onCast();
    }

    private void playWindupEffect() {
        caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation().add(0, 0.1, 0), 8, 0.5, 0.1, 0.5, 0.05);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.5f);
    }

    private void executeWarStomp() {
        playStompEffect();

        createCircularAoE(caster.getLocation(), shockwaveRadius, shockwaveHeight, 40)
                .onEnter((aoe, entity) -> {
                    if (!entity.equals(caster) && Spellbook.canAttack(caster, entity)) {
                        double physicalDamage = Spellbook.getVariedAttributeBasedDamage(data, caster, entity, true, Attribute.ADVANTAGE_PHYSICAL);
                        entity.damage(physicalDamage, caster, PDamageType.PHYSICAL);

                        int stunDuration = (int) Spellbook.getRangedValue(data, caster, entity, Attribute.ADVANTAGE_PHYSICAL, minStunDuration, maxStunDuration, "stunDuration");

                        entity.addEffect(caster, stunEffectData, stunDuration, 1);

                        playHitEffect(entity);
                    }
                });
    }

    private void playStompEffect() {
        caster.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, caster.getLocation().add(0, 0.1, 0), 30, 1.5, 0.1, 1.5, 0.1);
        caster.getWorld().spawnParticle(Particle.LARGE_SMOKE, caster.getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
    }

    private void playHitEffect(LivingEntity target) {
        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 0.8f);
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (currentTicks < 20) {
            return;
        }
        if (currentTicks == 20) { // After 1 second
            executeWarStomp();
            return;
        }
        if (currentTicks >= 40) {
            this.cancel();
            return;
        }
        double progress = currentTicks / 20.0;
        double currentRadius = shockwaveRadius * progress;

        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2 * i / 16);
            double x = Math.cos(angle) * currentRadius;
            double z = Math.sin(angle) * currentRadius;

            caster.getWorld().spawnParticle(Particle.DUST,
                    caster.getLocation().add(x, 0.2, z), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(org.bukkit.Color.GRAY, 1.0f));
        }
    }

}
