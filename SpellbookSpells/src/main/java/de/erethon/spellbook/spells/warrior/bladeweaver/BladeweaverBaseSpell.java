package de.erethon.spellbook.spells.warrior.bladeweaver;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import de.erethon.spellbook.traits.warrior.bladeweaver.BladeweaverBasicAttack;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class BladeweaverBaseSpell extends WarriorBaseSpell {

    protected static final String DEMON_FORM_TAG = "bladeweaver.demon_form";

    protected static final Color BLADEWEAVER_PRIMARY = Color.fromRGB(255, 165, 0);   // Orange
    protected static final Color BLADEWEAVER_SECONDARY = Color.fromRGB(255, 200, 50); // Golden yellow
    protected static final Color BLADEWEAVER_DEMON = Color.fromRGB(255, 69, 0);       // Red-Orange for demon form
    protected static final Color BLADEWEAVER_ACCENT = Color.fromRGB(255, 215, 0);     // Gold

    protected static final int MAX_RAZOR_MARK_STACKS = 5;
    protected static final int RAZOR_MARK_DURATION = 200; // 10 seconds in ticks

    protected int energyBuildUpFromUse = data.getInt("energyBuildUpFromUse", 5);
    protected double bonusHealthPerHit = data.getDouble("bonusHealthPerHit", 1.5);

    protected final EffectData razorMarkEffectData = Spellbook.getEffectData("RazorMark");

    public BladeweaverBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        // Bladeweaver abilities use energy for ult charge, not as a cost
        rageCost = 0;
    }

    /**
     * Checks if the caster is in demon form
     */
    protected boolean isInDemonForm() {
        return caster.getTags().contains(DEMON_FORM_TAG);
    }

    /**
     * Gets the current number of Razor Mark stacks on a target
     */
    protected int getRazorMarkStacks(LivingEntity target) {
        if (razorMarkEffectData == null) return 0;

        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == razorMarkEffectData) {
                return effect.getStacks();
            }
        }
        return 0;
    }

    /**
     * Adds Razor Mark stacks to a target using the SpellEffect system
     */
    protected void addRazorMarkStacks(LivingEntity target, int stacks) {
        if (razorMarkEffectData == null) return;

        int currentStacks = getRazorMarkStacks(target);
        int newStacks = Math.min(currentStacks + stacks, MAX_RAZOR_MARK_STACKS);

        // Apply or refresh the effect with new stack count
        target.addEffect(caster, razorMarkEffectData, RAZOR_MARK_DURATION, newStacks);
    }

    /**
     * Consumes all Razor Mark stacks from a target, returning the number consumed
     */
    protected int consumeRazorMarkStacks(LivingEntity target) {
        if (razorMarkEffectData == null) return 0;

        int stacks = 0;
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == razorMarkEffectData) {
                stacks = effect.getStacks();
                target.removeEffect(effect.data);
                playMarkConsumptionEffect(target, stacks);
                break;
            }
        }
        return stacks;
    }

    /**
     * Visual effect when Razor Marks are consumed
     */
    protected void playMarkConsumptionEffect(LivingEntity target, int stacks) {
        Location loc = target.getLocation().add(0, 1.5, 0);
        Particle.DustOptions goldDust = new Particle.DustOptions(BLADEWEAVER_ACCENT, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, loc, stacks * 5, 0.3, 0.3, 0.3, 0, goldDust);
        target.getWorld().spawnParticle(Particle.END_ROD, loc, stacks * 2, 0.2, 0.2, 0.2, 0.1);
        target.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 1.5f + (stacks * 0.1f));
    }

    /**
     * Gets the appropriate color based on demon form status
     */
    protected Color getThemeColor() {
        return isInDemonForm() ? BLADEWEAVER_DEMON : BLADEWEAVER_PRIMARY;
    }

    /**
     * Grants bonus health to the caster for hitting an enemy.
     * This hooks into the BladeweaverBasicAttack trait if present.
     */
    protected void grantBonusHealthForHit() {
        grantBonusHealth(bonusHealthPerHit);
    }

    /**
     * Grants a specific amount of bonus health to the caster.
     * This hooks into the BladeweaverBasicAttack trait if present.
     */
    protected void grantBonusHealth(double amount) {
        for (SpellTrait trait : caster.getActiveTraits()) {
            if (trait instanceof BladeweaverBasicAttack bladeweaverTrait) {
                bladeweaverTrait.addBonusHealth(amount);
                return;
            }
        }
    }

    @Override
    protected void onAfterCast() {
        if (energyBuildUpFromUse > 0 && !isInDemonForm()) {
            caster.addEnergy(energyBuildUpFromUse);
        }
        super.onAfterCast();
    }

    @Override
    protected boolean onPrecast() {
        // Bladeweaver abilities don't consume rage, so skip the parent check
        return true;
    }
}
