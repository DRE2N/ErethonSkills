package de.erethon.hecate;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hephaestus.HItem;
import de.erethon.hephaestus.HItemBehaviour;
import de.erethon.hephaestus.HItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;


public class TestBehaviour extends HItemBehaviour {

    public TestBehaviour(HItem item) {
        super(item);
    }

    @Override
    public void onRightClick(ItemStack stack, Player player, PlayerInteractEvent event) {
        MessageUtil.sendActionBarMessage(player, "Right click");
    }

    @Override
    public void onPickup(ItemStack stack, Player player, PlayerAttemptPickupItemEvent event) {
        MessageUtil.sendActionBarMessage(player, "DIRT!");
    }

    @Override
    public boolean onMine(ItemStack stack, Level world, BlockState state, BlockPos pos, net.minecraft.world.entity.player.Player miner) {
        MessageUtil.sendActionBarMessage((Player) miner.getBukkitEntity(), "Mine!");
        return super.onMine(stack, world, state, pos, miner);
    }
}
