package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.entity.LivingEntity;

public class TestSpell extends SpellbookSpell {

    public TestSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return true;
    }

    @Override
    protected boolean onCast() {
        caster.sendMessage("TestSpell. Hui!");
        caster.setVelocity(caster.getLocation().getDirection().multiply(data.getDouble("dashMultiplier", 5.0)));
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.setCooldown(data);
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onTickFinish() {
    }
}
