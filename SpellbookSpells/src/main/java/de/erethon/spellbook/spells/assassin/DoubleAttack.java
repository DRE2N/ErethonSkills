package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class DoubleAttack extends AssassinBaseSpell {

    private final int duration = data.getInt("duration", 200);

    public DoubleAttack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = duration;
        return super.onCast();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
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

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        return super.getPlaceholders(caster);
    }
}
