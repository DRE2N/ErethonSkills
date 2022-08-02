package de.erethon.spellbook.spells.assassin;

import de.erethon.spellbook.SpellData;
import de.erethon.spellbook.caster.SpellCaster;
import org.bukkit.entity.LivingEntity;

public class Dash extends AssassinBaseSpell {

    public Dash(SpellCaster caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        LivingEntity entity = caster.getEntity();
        if (entity.getLocation().getPitch() < -10) {
            caster.sendActionbar("<red>Ungültiger Winkel.");
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected boolean onCast() {
        caster.getEntity().setVelocity(caster.getEntity().getLocation().getDirection().multiply(data.getDouble("dashMultiplier", 2.0)));
        return true;
    }

    @Override
    protected void onAfterCast() {
        super.onAfterCast();
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onTickFinish() {
    }
}

