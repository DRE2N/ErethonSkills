package de.erethon.hecate.util;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hecate.Hecate;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class ResourcepackHandler implements Listener {

    private final Hecate plugin = Hecate.getInstance();
    private final ResourcepackCallback callback;
    private final Player player;
    private static final String RESOURCEPACK_URL = "https://github.com/DRE2N/Resourcepack/releases/download/latest/Erethon-Resourcepack.zip";
    private static final String ERETHON_MM = "<gradient:red:dark_red><st>       </st></gradient><dark_gray>]<gray><st> </st> <#ff3333><b>Erethon</b><gray> <st> </st><dark_gray>[<gradient:dark_red:red><st>       </st></gradient>";
    private int declineCount = 0;

    public ResourcepackHandler(Player player, ResourcepackCallback callback) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.callback = callback;
        this.player = player;
        Title title = Title.title(Component.empty(), Component.text("Loading resource pack...", NamedTextColor.GRAY));
        player.showTitle(title);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100000, 127, true, false, false));
        sendPack(player);
    }

    private void sendPack(Player player) {
        @NotNull CompletableFuture<ResourcePackInfo> pack = ResourcePackInfo.resourcePackInfo().uri(URI.create(RESOURCEPACK_URL)).computeHashAndBuild();
        Component message = MiniMessage.miniMessage().deserialize("<br>" + ERETHON_MM +
                "<br><br><red>A resource pack is required to play on Erethon." +
                "<br><gray>To offer you a special experience, Erethon makes heavy use of resource packs" +
                "<br><gray>to make our awesome and interesting features possible." +
                "<br><green><b>Please accept the resource pack.</b>" +
                "<br><gray><i>You will only have to do this once every update.");
        pack.thenAccept(info -> {
            player.sendResourcePacks(ResourcePackRequest.resourcePackRequest().required(false).replace(true).packs(info).prompt(message));
            Hecate.log("Sending resource pack to " + player.getName() + " with hash " + info.hash() + " and UUID " + info.id());
        });
        pack.orTimeout(10, java.util.concurrent.TimeUnit.SECONDS).exceptionally(throwable -> {
            Hecate.log("Failed to send resource pack to " + player.getName() + ". Timed out. Did the GitHub action fail?");
            finishApply();
            return null;
        });
    }

    @EventHandler
    public void onResourcepackChange(PlayerResourcePackStatusEvent event) {
        if (player == event.getPlayer()) {
            if (declineCount > 3) {
                Hecate.log("Player " + player.getName() + " declined the resource pack too many times. Kicking.");
                Component kickMessage = MiniMessage.miniMessage().deserialize(ERETHON_MM +
                        "<br><br><b><red>Please ensure resource packs are enabled to play on Erethon.</b>" +
                        "<br><dark_gray><i>You have been kicked for declining the resource pack too many times.</i>" +
                        "<br><br><br><i><gold><b>How to enable resource packs:</b></i>" +
                        "<br><br><gold><b>1)</b> <gray>Click on the server in your server list." +
                        "<br><gold><b>2)</b> <gray>Click on <i><#dddddd>Edit</i><gray> at the bottom." +
                        "<br><gold><b>3)</b> <gray>Make sure <i><#dddddd>Server Resource Packs<gray></i> is <br><gray>set to <i><#dddddd>Enabled</i><gray> or <i><#dddddd>Prompt</i><gray>.<br><br>");
                player.kick(kickMessage);
                declineCount = 0;
                return;
            }
            if (event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_RELOAD
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.DISCARDED
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.INVALID_URL) {
                Hecate.log("Resource pack download failed for " + player.getName() + ". Trying again. Reason: " + event.getStatus().name());
                sendPack(player);
                declineCount++;
                return;
            }
            if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
                Hecate.log("Resource pack applied successfully for " + player.getName());
                finishApply();
                return;
            }
        }
    }

    public void finishApply() {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        callback.onResourcepackDone(player);
    }
}
