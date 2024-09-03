package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DontMoveBack extends PaladinBaseSpell {

    private final int duration = data.getInt("duration", 10);

    private final EffectData dontmoveback = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("DontMoveBackEffect");

    public DontMoveBack(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Set<LivingEntity> targets = new HashSet<>();
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(data.getDouble("range", 10))) {
            if (Spellbook.canAttack(caster, living)) {
                continue;
            }
            living.addEffect(caster, dontmoveback, duration, 1);
            targets.add(living);
        }
        triggerTraits(targets);
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        return super.getPlaceholders(c);
    }
}
