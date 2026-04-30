package de.erethon.spellbook.traits.ranger.druid;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class DruidBasicAttack extends SpellTrait {

    private final int maxMana = data.getInt("maxMana", 100);
    private final int manaPerAttack = data.getInt("manaPerAttack", 5);
    private final int seedDuration = data.getInt("seedDuration", 140);
    private final int seedStacks = data.getInt("seedStacks", 1);
    private final int maxSeedStacks = data.getInt("maxSeedStacks", 3);
    private final EffectData seed = Spellbook.getEffectData("DruidSeed");

    public DruidBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        caster.setMaxEnergy(maxMana);
    }

    @Override
    protected void onRemove() {
        caster.setMaxEnergy(0);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (!Spellbook.canAttack(caster, target)) {
            return super.onAttack(target, damage, type);
        }
        caster.addEnergy(manaPerAttack);
        addSeed(target);
        return super.onAttack(target, damage, type);
    }

    private void addSeed(LivingEntity target) {
        if (seed == null) {
            return;
        }
        int currentStacks = 0;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == seed) {
                currentStacks = effect.getStacks();
                break;
            }
        }
        target.addEffect(caster, seed, seedDuration, Math.min(seedStacks, Math.max(1, maxSeedStacks - currentStacks)));
        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 1.4, 0), 3, 0.25, 0.35, 0.25, 0.01);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.1, 0), 4, 0.2, 0.25, 0.2, 0,
            new Particle.DustOptions(Color.fromRGB(99, 174, 71), 0.8f));
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AZALEA_LEAVES_HIT, 0.25f, 1.4f);
    }
}
