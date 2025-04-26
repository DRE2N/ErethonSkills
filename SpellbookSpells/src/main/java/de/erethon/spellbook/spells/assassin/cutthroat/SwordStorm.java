package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class SwordStorm extends AssassinBaseSpell {

    // Rapidly attacks the target with a sword, dealing physical damage and applying a bleed effect.

    private final int ticksBetweenAttacks = data.getInt("ticksBetweenAttacks", 10);
    public int attacks = data.getInt("attacks", 5);
    private final int effectDuration = data.getInt("effectDuration", 5);
    private final int effectStacks = data.getInt("effectStacks", 1);

    private EffectData bleed = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Bleeding");


    private int currentAttack = 0;

    public SwordStorm(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentAttack > attacks || caster.getLocation().distanceSquared(target.getLocation()) > 9) {
                    cancel();
                    return;
                }
                caster.attack(target);
                target.addEffect(caster, bleed, effectDuration, effectStacks);
                triggerTraits(target);
                currentAttack++;
            }
        };
        runnable.runTaskTimer(Spellbook.getInstance().getImplementer(), 0, ticksBetweenAttacks);
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(String.format("%.2f", (double) ticksBetweenAttacks / 20), VALUE_COLOR));
        placeholderNames.add("timeBetweenAttacks");
        spellAddedPlaceholders.add(Component.text(attacks, VALUE_COLOR));
        placeholderNames.add("attacks");
        return super.getPlaceholders(c);
    }
}
