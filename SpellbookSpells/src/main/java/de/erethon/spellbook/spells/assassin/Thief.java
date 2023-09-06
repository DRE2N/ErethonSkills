package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Random;

public class Thief extends AssassinBaseSpell {

    Random random = new Random();
    EffectData effectData;

    public Thief(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    protected boolean onCast() {
        List<SpellEffect> datas = target.getEffects().stream().toList();
        if (datas.isEmpty()) {
            caster.sendParsedActionBar("<#ff0000>Das Ziel hat keine Effekte");
            return false;
        }
        effectData = datas.get(random.nextInt(datas.size())).data;
        caster.addEffect(caster, effectData, data.getInt("duration", 5) * 20, 1);
        target.removeEffect(effectData);
        caster.sendParsedActionBar("<green>Du hast " + effectData.getName() + " gestohlen!");
        return true;
    }

}
