package de.erethon.hecate.commands;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.hecate.Hecate;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TestCommand extends ECommand {

    public TestCommand() {
        setCommand("test");
        setAliases("tst");
        setMinArgs(0);
        setMaxArgs(999);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("This is a test command.");
        setPermission("hecate.test");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        Hecate plugin = Hecate.getInstance();
        long startTime = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            player.spawnParticle(Particle.GUST_EMITTER_LARGE, player.getLocation(), 1, 2, 2, 2, 0);
        }
        MessageUtil.log("Spawning particles took " + (System.nanoTime() - startTime) / 1_000_000 + " ms");
    }
}
