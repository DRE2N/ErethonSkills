package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class JudgementOfGod extends PaladinBaseSpell {

    public int duration = data.getInt("duration", 100);
    private final int range = data.getInt("range", 16);
    private final EffectData effectData = Spellbook.getEffectData("JudgeOfGodEffect");

    public JudgementOfGod(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(range);
    }

    @Override
    public boolean onCast() {
        target.addEffect(caster, effectData, duration, 1);
        target.playSound(Sound.sound(org.bukkit.Sound.BLOCK_BELL_USE, Sound.Source.RECORD, 1, 0));
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_BELL_USE, Sound.Source.RECORD, 1, 0));
        triggerTraits(target);
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        spellAddedPlaceholders.add(Component.text(range, VALUE_COLOR));
        return super.getPlaceholders(c);
    }
}
