package de.erethon.spellbook.spells.ranger.beastmaster;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import de.erethon.spellbook.spells.ranger.beastmaster.pet.RangerPet;
import de.erethon.spellbook.traits.ranger.beastmaster.SpawnPetTrait;
import de.erethon.spellbook.utils.RangerUtils;
import org.bukkit.entity.LivingEntity;

public class RangerPetBaseSpell extends RangerBaseSpell {

    protected SpawnPetTrait petTrait;
    protected RangerPet pet;

    public RangerPetBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        caster.getActiveTraits().forEach(trait -> {
            if (trait instanceof SpawnPetTrait spawnPetTrait) {
                petTrait = spawnPetTrait;
            }
        });
    }

    @Override
    protected boolean onPrecast() {
        if (!RangerUtils.hasPet(caster)) return false;
        pet = RangerUtils.getPet(caster);
        return super.onPrecast();
    }

}
