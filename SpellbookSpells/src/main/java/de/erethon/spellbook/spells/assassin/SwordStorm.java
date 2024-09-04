package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class SwordStorm extends AssassinBaseSpell {

    private final int ticksBetweenAttacks = data.getInt("ticksBetweenAttacks", 10);
    public int attacks = data.getInt("attacks", 5);
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
