package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RevealEnemies extends RangerBaseSpell {

    private final int range = data.getInt("range", 10);

    public RevealEnemies(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        Set<LivingEntity> affected = new HashSet<>();
        caster.getNearbyEntities(range, range, range).forEach(entity -> {
            if (!(entity instanceof LivingEntity)) {
                return;
            }
            if (!Spellbook.canAttack(caster, (LivingEntity) entity)) {
                return;
            }
            if (!((LivingEntity) entity).isInvisible()) {
                return;
            }
            ((LivingEntity) entity).setInvisible(false);
            affected.add((LivingEntity) entity);
        });
        triggerTraits(affected);
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(range, VALUE_COLOR));
        placeholderNames.add("range");
        return super.getPlaceholders(c);
    }
}
