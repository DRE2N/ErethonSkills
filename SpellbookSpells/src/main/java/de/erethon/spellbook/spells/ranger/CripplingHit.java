package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.List;

public class CripplingHit extends RangerBaseSpell{

    private final int nauseaDuration = data.getInt("nauseaDuration", 20);

    public CripplingHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(true);
    }

    @Override
    public boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDuration, 1, true, false));
        triggerTraits(Collections.singleton(target));
        triggerTraits(target);
        return true;
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(nauseaDuration, VALUE_COLOR));
        placeholderNames.add("nausea duration");
        return super.getPlaceholders(c);
    }
}
