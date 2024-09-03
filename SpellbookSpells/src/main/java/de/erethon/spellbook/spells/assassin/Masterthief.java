package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Random;

public class Masterthief extends AssassinBaseSpell {

    Random random = new Random();
    SpellData spell;

    public int duration = data.getInt("duration", 30);

    public Masterthief(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 1000;
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = duration; // Do this here for trait access
        List<SpellData> datas = target.getUsedSpells().keySet().stream().toList();
        if (datas.isEmpty()) {
            caster.sendParsedActionBar("<#ff0000>Das Ziel hat keine Spells eingesetzt.");
            return false;
        }
        spell = datas.get(random.nextInt(datas.size()));
        caster.addSpell(spell);
        triggerTraits(target);
        caster.sendParsedActionBar("<green>Du hast " + spell.getId() + " gestohlen!");
        return true;
    }

    @Override
    protected void cleanup() {
        caster.removeSpell(spell);
        caster.sendParsedActionBar("<gray>Dein gestohlener Spell " + spell.getId() + " ist ausgelaufen.");
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(duration, VALUE_COLOR));
        return super.getPlaceholders(c);
    }
}
