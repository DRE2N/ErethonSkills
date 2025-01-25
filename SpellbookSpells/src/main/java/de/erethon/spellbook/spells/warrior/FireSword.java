package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class FireSword extends WarriorBaseSpell {

    private final int bonusDamage = data.getInt("bonusDamage", 10);

    public FireSword(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        if (type == PDamageType.MAGIC) {
            return super.onDamage(target, damage, type);
        }
        //missing method target.damage(damage + bonusDamage, caster, PDamageType.MAGIC);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 1, new Particle.DustOptions(Color.ORANGE,3f));
        triggerTraits(target);
        return 0; // We can't deal damage twice in the same attack.
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(bonusDamage, VALUE_COLOR));
        placeholderNames.add("bonus damage");
        return super.getPlaceholders(c);
    }
}