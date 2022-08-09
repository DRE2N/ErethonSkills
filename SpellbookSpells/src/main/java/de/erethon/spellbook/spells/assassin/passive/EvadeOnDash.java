package de.erethon.spellbook.spells.assassin.passive;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.PassiveSpell;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class EvadeOnDash extends PassiveSpell {

    private final SpellData dashSpell = Bukkit.getServer().getSpellbookAPI().getLibrary().getSpellByID("Dash");
    private final int duration = data.getInt("duration", 20);
    private int counter = 0;
    private boolean isDashing = false;

    public EvadeOnDash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast(SpellbookSpell spell) {
        if (spell.getData() == dashSpell) {
            isDashing = true;
        }
        return true;
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, DamageType type) {
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
