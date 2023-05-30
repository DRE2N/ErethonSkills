package de.erethon.spellbook.traits;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.entity.LivingEntity;

public abstract class ClassMechanic extends SpellTrait {

    private String statusText;

    public ClassMechanic(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    public String getStatusText() {
        return statusText;
    }

}
