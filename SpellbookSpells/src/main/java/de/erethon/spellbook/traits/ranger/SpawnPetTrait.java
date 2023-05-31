package de.erethon.spellbook.traits.ranger;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.ranger.pet.RangerPet;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class SpawnPetTrait extends SpellTrait {

    private final double attributeModifier = data.getDouble("attributeModifier", 0.8);

    private RangerPet pet;

    public SpawnPetTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        pet = new RangerPet(caster, caster.getWorld(), EntityType.PIG);
        pet.teleport(caster.getLocation().getBlockX(), caster.getLocation().getBlockY(), caster.getLocation().getBlockZ());
        pet.setScaledAttributes(attributeModifier);
        pet.addToWorld();
    }

    @Override
    protected void onRemove() {
        pet.remove();
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (pet.isShouldAttackAutomatically()) {
            pet.makeAttack(target);
        }
        return super.onAttack(target, damage, type);
    }


}
