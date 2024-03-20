package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;

public class SharedPain extends RangerPetBaseSpell {

    private final double petDamagePercentage = data.getDouble("petDamagePercentages", 0.5);

    public SharedPain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("duration", 200);
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
}
