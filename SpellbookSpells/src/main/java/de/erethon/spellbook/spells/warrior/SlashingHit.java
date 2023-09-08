package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class SlashingHit extends WarriorBaseSpell {

    private final EffectData bleeding = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Bleeding");
    public double damageMultiplier = 1.0;
    public double bleedingStackMultiplier = 1.0;

    public SlashingHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(3);
    }

    @Override
    protected boolean onCast() {
        target.addEffect(caster, bleeding, (int) (data.getInt("baseDuration", 40) + Spellbook.getScaledValue(data, caster, target, Attribute.ADV_PHYSICAL)), (int) (data.getInt("stacks", 1) * bleedingStackMultiplier));
        target.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADV_PHYSICAL) * damageMultiplier, caster, DamageType.PHYSICAL);
        return super.onCast();
    }
}
