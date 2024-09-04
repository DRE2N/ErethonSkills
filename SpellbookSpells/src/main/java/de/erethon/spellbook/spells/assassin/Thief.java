package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.spells.EntityTargetSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Random;

public class Thief extends AssassinBaseSpell {

    private final int effectDuration = data.getInt("effectDuration", 5);

    private final Random random = new Random();

    public Thief(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        List<SpellEffect> datas = target.getEffects().stream().toList();
        if (datas.isEmpty()) {
            caster.sendParsedActionBar("<#ff0000>Das Ziel hat keine Effekte");
            return false;
        }
        EffectData effectData = datas.get(random.nextInt(datas.size())).data;
        caster.addEffect(caster, effectData, effectDuration * 20, 1);
        target.removeEffect(effectData);
        triggerTraits(target);
        caster.sendParsedActionBar("<green>Du hast " + effectData.getName() + " gestohlen!");
        return true;
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(effectDuration, VALUE_COLOR));
        placeholderNames.add("effectDuration");
        return super.getPlaceholders(c);
    }
}
