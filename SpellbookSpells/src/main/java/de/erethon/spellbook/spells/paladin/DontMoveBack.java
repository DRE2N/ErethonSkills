package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class DontMoveBack extends PaladinBaseSpell {

    private final EffectData dontmoveback = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("DontMoveBackEffect");

    public DontMoveBack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        Set<LivingEntity> targets = new HashSet<>();
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(data.getDouble("range", 10))) {
            if (Spellbook.canAttack(caster, living)) {
                continue;
            }
            living.addEffect(caster, dontmoveback, data.getInt("duration", 100), 1);
            targets.add(living);
        }
        triggerTraits(targets);
        return super.onCast();
    }
}
