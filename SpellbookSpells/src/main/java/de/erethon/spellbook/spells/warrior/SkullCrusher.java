package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkullCrusher extends WarriorBaseSpell {

    public SkullCrusher(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(2);
    }

    @Override
    protected boolean onCast() {
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20, 1));
        target.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADV_PHYSICAL), caster, DamageType.PHYSICAL);
        target.playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_DESTROY, Sound.Source.RECORD, 1, 1));
        caster.playSound(Sound.sound(org.bukkit.Sound.BLOCK_ANVIL_DESTROY, Sound.Source.RECORD, 0.8f, 1));
        return super.onCast();
    }
}
