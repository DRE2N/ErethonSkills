package de.erethon.hecate.ui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.data.HPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CharacterSelection implements Listener {

    private final DatabaseManager dbManager;

    public CharacterSelection(HPlayer hPlayer) {
        this.dbManager = Hecate.getInstance().getDatabaseManager();
        Hecate.getInstance().getServer().getPluginManager().registerEvents(this, Hecate.getInstance());
    }

}