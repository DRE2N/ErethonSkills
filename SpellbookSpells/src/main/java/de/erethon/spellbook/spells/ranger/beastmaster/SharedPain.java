package de.erethon.spellbook.spells.ranger.beastmaster;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class SharedPain extends RangerPetBaseSpell {

    private final double petDamagePercentage = data.getDouble("petDamagePercentages", 0.5);

    public SharedPain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        pet.getBukkitLivingEntity().getWorld().playSound(pet.getBukkitLivingEntity().getLocation(), Sound.ENTITY_WOLF_GROWL, SoundCategory.RECORDS, 1, 1);
        return super.onCast();
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        pet.getBukkitLivingEntity().damage(Math.max(0, damage * petDamagePercentage), attacker, type);
        return super.onDamage(attacker, Math.max(0, damage - (damage * petDamagePercentage)), type);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(petDamagePercentage, VALUE_COLOR));
        placeholderNames.add("pet damage percentage");
        return super.getPlaceholders(c);
    }

}
