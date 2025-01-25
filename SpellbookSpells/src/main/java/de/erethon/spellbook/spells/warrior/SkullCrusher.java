package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SkullCrusher extends WarriorBaseSpell {

    public SkullCrusher(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(2);
    }

    @Override
    public boolean onCast() {
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 1));
        //missing method target.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL), caster, PDamageType.PHYSICAL);
        target.playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_DESTROY, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_DESTROY, Sound.Source.RECORD, 0.8f, 1));
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL), ATTR_PHYSICAL_COLOR));
        placeholderNames.add("damage");
        return super.getPlaceholders(c);
    }
}
