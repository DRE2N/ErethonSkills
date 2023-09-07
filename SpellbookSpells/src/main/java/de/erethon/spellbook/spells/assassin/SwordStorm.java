package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

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
    protected boolean onCast() {
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
}
