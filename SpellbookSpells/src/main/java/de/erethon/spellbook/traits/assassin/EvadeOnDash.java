package de.erethon.spellbook.traits.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.api.TraitTrigger;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class EvadeOnDash extends SpellTrait {

    private final SpellData dashSpell = Bukkit.getServer().getSpellbookAPI().getLibrary().getSpellByID("Dash");
    private final int duration = data.getInt("duration", 20);
    private int counter = 0;
    private boolean isDashing = false;

    public EvadeOnDash(TraitData traitData, LivingEntity caster) {
        super(traitData, caster);
    }

    @Override
    protected void onTrigger(TraitTrigger trigger) {
        if (trigger.getId() == 0) {
            isDashing = true;
        }
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        return super.onDamage(attacker, (isDashing ? 0 : damage), type);
    }

    @Override
    protected void onTick() {
        if (isDashing) {
            counter++;
            if (counter >= duration) {
                counter = 0;
                isDashing = false;
            }
        }
    }
}
