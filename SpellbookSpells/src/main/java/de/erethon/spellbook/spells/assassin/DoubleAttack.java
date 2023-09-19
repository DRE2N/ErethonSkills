package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class DoubleAttack extends SpellbookSpell {

    public DoubleAttack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        keepAliveTicks = data.getInt("duration", 200);
        return super.onCast();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        // TODO: This is kinda bad, but uh, it works.
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                caster.attack(target);
            }
        };
        runnable.runTaskLater(Spellbook.getInstance().getImplementer(), 5);
        triggerTraits(target);
        return super.onAttack(target, damage, type);
    }
}
