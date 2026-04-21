package de.erethon.spellbook.spells.warrior.vanguard;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class UnflinchingStance extends VanguardBaseSpell {

    private final int minDuration = data.getInt("minDuration", 4) * 20;
    private final int maxDuration = data.getInt("maxDuration", 8) * 20;
    private final int minResistanceStacks = data.getInt("minResistanceStacks", 2);
    private final int maxResistanceStacks = data.getInt("maxResistanceStacks", 4);
    private final int minStabilityStacks = data.getInt("minStabilityStacks", 1);
    private final int maxStabilityStacks = data.getInt("maxStabilityStacks", 2);

    private final EffectData resistanceEffectData = Spellbook.getEffectData("Resistance");
    private final EffectData stabilityEffectData = Spellbook.getEffectData("Stability");

    private final List<BlockDisplay> shieldDisplays = new ArrayList<>();

    public UnflinchingStance(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minDuration, maxDuration);
    }

    @Override
    public boolean onCast() {
        int duration = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minDuration, maxDuration, "duration");
        int resistanceStacks = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minResistanceStacks, maxResistanceStacks, "resistanceStacks");
        int stabilityStacks = (int) Spellbook.getRangedValue(data, caster, Attribute.ADVANTAGE_PHYSICAL, minStabilityStacks, maxStabilityStacks, "stabilityStacks");

        caster.addEffect(caster, resistanceEffectData, duration, resistanceStacks);
        caster.addEffect(caster, stabilityEffectData, duration, stabilityStacks);

        spawnShieldDisplays();
        playStanceEffects();

        caster.getTags().add("warrior.unflinching_stance");

        return super.onCast();
    }

    private void spawnShieldDisplays() {
        Location center = caster.getLocation().add(0, 0.5, 0);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            double angle = Math.PI / 2 * i;
            float x = (float) (Math.cos(angle) * 1.0);
            float z = (float) (Math.sin(angle) * 1.0);
            BlockDisplay block = center.getWorld().spawn(center.clone().add(x, 0, z), BlockDisplay.class, d -> {
                d.setBlock(Material.IRON_BLOCK.createBlockData());
                d.setPersistent(false);
                d.setGlowing(true);
                d.setInterpolationDuration(2);
                d.setTeleportDuration(2);
                d.setTransformation(new Transformation(
                    new Vector3f(-0.2f, -0.2f, -0.2f),
                    new Quaternionf(),
                    new Vector3f(0.4f, 0.4f, 0.4f),
                    new Quaternionf()
                ));
            });
            shieldDisplays.add(block);
        }
    }

    @Override
    protected void onTick() {
        super.onTick();

        // Orbit shield displays around caster
        Location center = caster.getLocation().add(0, 0.5, 0);
        float time = currentTicks * 0.08f;
        for (int i = 0; i < shieldDisplays.size(); i++) {
            BlockDisplay block = shieldDisplays.get(i);
            if (block == null || !block.isValid()) continue;
            double angle = Math.PI / 2 * i + time;
            double x = Math.cos(angle) * 1.0;
            double z = Math.sin(angle) * 1.0;
            block.teleport(center.clone().add(x, 0, z));
        }

        if (currentTicks % 8 == 0) {
            playAuraEffect();
        }
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        return super.onDamage(attacker, damage, type);
    }

    public void onCCAbsorbed() {
        playAbsorptionEffect();
    }

    private void playStanceEffects() {
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, caster.getLocation().add(0, 1, 0), 30, 0.8, 1.0, 0.8, 0.1);
        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 25, 0.6, 0.8, 0.6, 0,
            new Particle.DustOptions(Color.SILVER, 1.5f));
        caster.getWorld().spawnParticle(Particle.FIREWORK, caster.getLocation().add(0, 0.5, 0), 8, 0.4, 0.2, 0.4, 0.05);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 0.6f, 1.4f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.8f);
    }

    private void playAuraEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            Location p = loc.clone().add(Math.cos(angle) * 1.2, 0, Math.sin(angle) * 1.2);
            loc.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.SILVER, 0.8f));
        }
    }

    private void playAbsorptionEffect() {
        // Flare all shield displays white momentarily
        for (BlockDisplay block : shieldDisplays) {
            if (block != null && block.isValid()) {
                block.setGlowColorOverride(Color.WHITE);
            }
        }
        Location loc = caster.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 15, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (BlockDisplay block : shieldDisplays) {
                    if (block != null && block.isValid()) {
                        block.setGlowColorOverride(null);
                    }
                }
            }
        }.runTaskLater(Spellbook.getInstance().getImplementer(), 5L);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.getTags().remove("warrior.unflinching_stance");
    }

    @Override
    protected void cleanup() {
        for (BlockDisplay block : shieldDisplays) {
            if (block != null && block.isValid()) block.remove();
        }
        shieldDisplays.clear();
    }
}
