package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Collections;
import java.util.List;

public class PoisonArrow extends ProjectileRelatedSkill {

    private final EffectData poison = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Poison");

    private final int poisonDuration = data.getInt("poisonDuration", 100);
    private final int poisonStacks = data.getInt("poisonStacks", 1);

    public PoisonArrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        trailColor = Color.GREEN;
        affectedArrows = spellData.getInt("affectedArrows", 1);
    }

    @Override
    protected void onHit(ProjectileHitEvent event, LivingEntity living) {
        living.addEffect(caster, poison, poisonDuration, poisonStacks);
        triggerTraits(Collections.singleton(living));
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(poisonDuration, VALUE_COLOR));
        placeholderNames.add("poison duration");
        spellAddedPlaceholders.add(Component.text(poisonStacks, VALUE_COLOR));
        placeholderNames.add("poison stacks");
        spellAddedPlaceholders.add(Component.text(affectedArrows, VALUE_COLOR));
        placeholderNames.add("affected arrows");
        return super.getPlaceholders(c);
    }
}
