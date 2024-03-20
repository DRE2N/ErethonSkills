package de.erethon.spellbook.traits.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.traits.ClassMechanic;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class RageMechanic extends ClassMechanic {

    private final int ragePerHit = data.getInt("ragePerHit", 1);
    private final int rageLevelRequired = data.getInt("requiredRageLevel", 99);
    private final SpellData spellToOverload = Bukkit.getServer().getSpellbookAPI().getLibrary().getSpellByID(data.getString("spellToOverload"));
    private final SpellData overLoadSpell = Bukkit.getServer().getSpellbookAPI().getLibrary().getSpellByID(data.getString("overloadSpell"));

    public RageMechanic(TraitData data, LivingEntity caster) {
        super(data, caster);
        if (spellToOverload == null) {
            Bukkit.getLogger().warning("Spell overload" + data.getString("spellToOverload") + " not found.");
        }
    }

    @Override
    protected void onAdd() {
        caster.setMaxEnergy(100);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        caster.addEnergy(ragePerHit);
        return super.onAttack(target, damage, type);
    }

    @Override
    protected SpellbookSpell onSpellCast(SpellbookSpell casted) {
        if (casted.getData() == spellToOverload) {
            if (caster.getEnergy() < rageLevelRequired) {
                return casted;
            }
            caster.setEnergy(0);
            return overLoadSpell.queue(caster);
        }
        return super.onSpellCast(casted);
    }
}
