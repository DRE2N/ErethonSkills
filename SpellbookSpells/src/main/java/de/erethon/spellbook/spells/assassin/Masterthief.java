package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Random;

public class Masterthief extends EntityTargetSpell {

    Random random = new Random();
    SpellData spell;

    public Masterthief(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 30) * 10;
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        List<SpellData> datas = targetEntity.getUsedSpells().keySet().stream().toList();
        if (datas.isEmpty()) {
            caster.sendParsedActionBar("<#ff0000>Das Ziel hat keine Spells eingesetzt.");
            return false;
        }
        spell = datas.get(random.nextInt(datas.size()));
        caster.addSpell(spell);
        caster.sendParsedActionBar("<green>Du hast " + spell.getId() + " gestohlen!");
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

    @Override
    protected void onTickFinish() {
        caster.removeSpell(spell);
        caster.sendParsedActionBar("<gray>Dein gestohlener Spell " + spell.getId() + " ist ausgelaufen.");
    }
}
