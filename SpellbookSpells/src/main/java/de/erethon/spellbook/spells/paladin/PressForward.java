package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public class PressForward extends AoEBaseSpell {

    private final int duration = data.getInt("duration", 100);
    private final int stacks = data.getInt("stacks", 1);

    private final EffectData speed = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Speed");
    private final Set<LivingEntity> affected = new HashSet<>();

    public PressForward(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("keepAliveTicks", 100);
    }


    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        return super.onCast();
    }

    @Override
    protected void onEnter(LivingEntity entity) {
        if (!affected.contains(entity) && !Spellbook.canAttack(caster, entity)) {
            entity.addEffect(caster, speed, duration, stacks);
            triggerTraits(affected);
            affected.add(entity);
        }
    }
}
