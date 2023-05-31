package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Random;

public class Thief extends EntityTargetSpell {

    Random random = new Random();
    EffectData effectData;

    public Thief(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    protected boolean onCast() {
        List<SpellEffect> datas = targetEntity.getEffects().stream().toList();
        if (datas.isEmpty()) {
            caster.sendParsedActionBar("<#ff0000>Das Ziel hat keine Effekte");
            return false;
        }
        effectData = datas.get(random.nextInt(datas.size())).data;
        caster.addEffect(caster, effectData, data.getInt("duration", 5) * 20, 1);
        targetEntity.removeEffect(effectData);
        caster.sendParsedActionBar("<green>Du hast " + effectData.getName() + " gestohlen!");
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

}
