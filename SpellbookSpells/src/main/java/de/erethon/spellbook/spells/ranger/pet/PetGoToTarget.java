package de.erethon.spellbook.spells.ranger.pet;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.RangerUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class PetGoToTarget extends SpellbookSpell {

    private final int range = data.getInt("range", 32);
    private Location target;

    public PetGoToTarget(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        if (!RangerUtils.hasPet(caster)) return false;
        Block targetBlockExact = caster.getTargetBlockExact(range);
        if (targetBlockExact == null) {
            return false;
        }
        target = targetBlockExact.getLocation().add(0,1,0);
        return true;
    }

    @Override
    protected boolean onCast() {
        RangerUtils.getPet(caster).goToLocation(target.getBlockX(), target.getBlockY(), target.getBlockZ());
        return true;
    }
}
