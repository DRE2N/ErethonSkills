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
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

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
        for (int xMod = -size; xMod <= size; xMod++) {
            for (int zMod = -size; zMod <= size; zMod++) {
                BlockPos pos = new BlockPos(ox + xMod, oy, oz + zMod);
                ItemFrame itemFrame = new ItemFrame(world, pos, Direction.UP);
                ItemStack map = MapItem.create(world, 0, 0, (byte) 4, false, false);
                ids.add(MapItem.getMapId(map));
                itemFrame.setItem(map);
                itemFrame.setInvisible(true);
                ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(itemFrame);
                ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(itemFrame.getId(), itemFrame.getEntityData(), true);
                ClientboundMoveEntityPacket moveEntityPacket = new ClientboundMoveEntityPacket.Rot(itemFrame.getId(), (byte) 0, (byte) -64, false);
                ServerPlayer nmsplayer = ((CraftPlayer) player).getHandle();
                nmsplayer.connection.send(addEntityPacket);
                nmsplayer.connection.send(dataPacket);
                nmsplayer.connection.send(moveEntityPacket);
            }
        }
        player.sendRawMessage("Created " + ids.size() + " frames.");
    }


}

