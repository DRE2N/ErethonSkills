package de.erethon.spellbook.spells.assassin;

import com.destroystokyo.paper.ParticleBuilder;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.List;

public class Assassination extends AssassinBaseSpell {

    private final NamespacedKey key = new NamespacedKey("spellbook", "assassination");
    private AttributeModifier critModifier;
    private AttributeModifier critChanceModifier;
    private AttributeModifier resistanceModifier;
    private final double critChance = data.getInt("critChance", 50);
    private final double critAmount = data.getInt("critAmount", 20);
    private final double resistance = data.getInt("resistances", -20);

    public Assassination(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 10) * 20;
    }

    @Override
    public boolean onCast() {
        critModifier = new AttributeModifier(key, critAmount, AttributeModifier.Operation.ADD_NUMBER);
        critChanceModifier = new AttributeModifier(key, critChance, AttributeModifier.Operation.ADD_NUMBER);
        resistanceModifier = new AttributeModifier(key, resistance, AttributeModifier.Operation.ADD_NUMBER);
        caster.getAttribute(Attribute.STAT_CRIT_DMG).addModifier(critModifier);
        caster.getAttribute(Attribute.STAT_CRIT_CHANCE).addModifier(critChanceModifier);
        return super.onCast();
    }

    @Override
    protected void onTick() {
        for (int i = 0; i < 5; i++) {
            new ParticleBuilder(Particle.DUST).color(255, 0,50).allPlayers().location(caster.getLocation().add(0, -1, 0)).spawn();
        }
    }

    @Override
    protected void cleanup() {
        caster.getAttribute(Attribute.STAT_CRIT_DMG).removeModifier(critModifier);
        caster.getAttribute(Attribute.STAT_CRIT_CHANCE).removeModifier(critChanceModifier);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(critChance, VALUE_COLOR));
        spellAddedPlaceholders.add(Component.text(critAmount, VALUE_COLOR));
        spellAddedPlaceholders.add(Component.text(resistance, VALUE_COLOR));
        return super.getPlaceholders(caster);
    }
}

