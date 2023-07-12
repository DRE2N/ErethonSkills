package de.erethon.spellbook.traits.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ForestKnowledge extends SpellTrait implements Listener {

    private final AttributeModifier speedBonus;
    private final Set<Biome> forestBiomes = new HashSet<>(Arrays.asList(
            Biome.DARK_FOREST,
            Biome.FOREST,
            Biome.CRIMSON_FOREST,
            Biome.CRIMSON_FOREST,
            Biome.FLOWER_FOREST,
            Biome.BIRCH_FOREST,
            Biome.OLD_GROWTH_PINE_TAIGA,
            Biome.OLD_GROWTH_BIRCH_FOREST,
            Biome.OLD_GROWTH_SPRUCE_TAIGA,
            Biome.BAMBOO_JUNGLE,
            Biome.JUNGLE,
            Biome.SPARSE_JUNGLE,
            Biome.WINDSWEPT_FOREST,
            Biome.WARPED_FOREST,
            Biome.LUSH_CAVES
    ));
    private boolean isInForest = false;

    public ForestKnowledge(TraitData data, LivingEntity caster) {
        super(data, caster);
        speedBonus = new AttributeModifier("forest_knowledge", data.getDouble("movementSpeedBonus", 0.1), AttributeModifier.Operation.ADD_NUMBER);
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (event.getPlayer() != caster) return;
        if (!event.hasChangedBlock()) return;
        if (isInForest && !forestBiomes.contains(event.getTo().getBlock().getBiome())) {
            isInForest = false;
            caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(speedBonus);
        }
        if (forestBiomes.contains(event.getTo().getBlock().getBiome())) {
            isInForest = true;
            caster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(speedBonus);
        }
    }

    @Override
    protected void onAdd() {
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
    }
}
