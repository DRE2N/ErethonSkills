package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TestCommand extends ECommand {

    private static final int PIXELS_PER_FRAME = 128;

    public TestCommand() {
        setCommand("test");
        setAliases("s");
        setMinArgs(0);
        setMaxArgs(3);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Help.");
        setPermission("hecate.reload");
    }

    public ArrayList<Integer> ids = new ArrayList<>();

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Hecate plugin = Hecate.getInstance();
        Player player = (Player) commandSender;
        File imageFile = new File(plugin.getDataFolder(), "test.png");
        BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        spawnFrames(player, player.getTargetBlock(20).getLocation(), Integer.parseInt(args[1]));
    }


    private void spawnFrames(Player player, Location center, int size) {
        ids.clear();
        Level world = ((CraftWorld) center.getWorld()).getHandle();
        double ox = center.getBlockX();
        double oy = center.getBlockY() + 1;
        double oz = center.getBlockZ();
        BlockPos pos = new BlockPos(ox , oy, oz);
        ItemFrame itemFrame = new ItemFrame(world, pos, Direction.UP);
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        itemFrame.setItem(map);
        itemFrame.setInvisible(true);
        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(itemFrame);
        ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(itemFrame.getId(), itemFrame.getEntityData(), true);
        ClientboundMoveEntityPacket moveEntityPacket = new ClientboundMoveEntityPacket.Rot(itemFrame.getId(), (byte) 0, (byte) 0, false);
        ClientboundMoveEntityPacket movePos = new ClientboundMoveEntityPacket.Pos(itemFrame.getId(), (short) 4, (short) 1, (short) 1,  false);
        ServerPlayer nmsplayer = ((CraftPlayer) player).getHandle();
        nmsplayer.connection.send(addEntityPacket);
        //nmsplayer.connection.send(dataPacket);
        nmsplayer.connection.send(moveEntityPacket);
        nmsplayer.connection.send(movePos);

        player.sendRawMessage("Created " + ids.size() + " frames.");
        BukkitRunnable runnable = new BukkitRunnable() {

            int i = 0;
            @Override
            public void run() {
                ClientboundMoveEntityPacket move = new ClientboundMoveEntityPacket.Pos(itemFrame.getId(), (short) i, (short) i, (short) i,  false);
                i++;
                nmsplayer.connection.send(move);
                player.sendRawMessage("Moving...");
            }
        };
        runnable.runTaskTimer(Hecate.getInstance(), 2, 2);
    }


}

