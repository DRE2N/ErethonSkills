package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.SpellbookCommonMessages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class SpiritGrove extends RangerBaseSpell {

    private final int range = data.getInt("radius", 16);

    private HashMap<Location, BlockData> blocks = new HashMap<>();
    private boolean finishedChannel = false;

    public SpiritGrove(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = data.getInt("duration", 10);
        tickInterval = 20;
        channelDuration = data.getInt("channelDuration", 40);
    }

    @Override
    protected boolean onPrecast() {
        Block targetBlock = caster.getTargetBlockExact(data.getInt("range", 32));
        if (targetBlock == null || targetBlock.getType().isAir()) {
            caster.sendParsedActionBar(SpellbookCommonMessages.NO_TARGET);
            return false;
        }
        return super.onPrecast();
    }

    @Override
    protected void onChannelFinish() {
        finishedChannel = true;
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                if (x * x + z * z <= range * range) {
                    Block block = caster.getLocation().getBlock().getRelative(x, 0, z);
                    if (block.getType().isAir() && block.getLightFromSky() > 8) {
                        int random = (int) (Math.random() * 100);
                        BlockData data;
                        if (random < 50) {
                            data = Material.SHORT_GRASS.createBlockData();
                        } else if (random < 60) {
                            data = Material.LEAF_LITTER.createBlockData();
                        } else if (random < 70) {
                            data = Material.FERN.createBlockData();
                        } else if (random < 80) {
                            data = Material.TALL_GRASS.createBlockData();
                        } else if (random < 90) {
                            data = Material.POPPY.createBlockData();
                        } else {
                            data = Material.DANDELION.createBlockData();
                        }
                        blocks.put(block.getLocation(), data);
                    }
                }
            }
        }
        for (Player player : caster.getLocation().getNearbyPlayers(48)) {
            sendFakeBlock(player);
        }
    }

    @Override
    public boolean onCast() {
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (!finishedChannel) {
            return;
        }
        for (Player player : caster.getLocation().getNearbyPlayers(range)) {
            if (Spellbook.canAttack(caster, player)) {
                player.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, player, true, Attribute.ADVANTAGE_MAGICAL));
                Particle particle = Particle.DUST;
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
            } else {
                player.heal(Spellbook.getScaledValue(data, caster, player, Attribute.STAT_HEALINGPOWER));
                Particle particle = Particle.HEART;
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2);
            }
        }
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        if (!finishedChannel) {
            return;
        }
        for (Player player : caster.getLocation().getNearbyPlayers(48)) {
            resetFakeBlocks(player);
        }
    }

    private void sendFakeBlock(Player player) {
        for (Location loc : blocks.keySet()) {
            player.sendBlockChange(loc, blocks.get(loc));
        }
    }

    private void resetFakeBlocks(Player player) {
        for (Location loc : blocks.keySet()) {
            Block block = loc.getBlock();
            player.sendBlockChange(loc, block.getBlockData());
        }
        blocks.clear();
    }
}
