package me.superuser.kamod.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class MuteManager {
    private Plugin plugin;
    private static Map<Player, Long> mutedPlayers;
    public MuteManager(Plugin plugin) {
        this.plugin = plugin;
        this.mutedPlayers = new HashMap<>();
    }

    public void mutePlayer(Player player, long time) {
        mutedPlayers.put(player, System.currentTimeMillis() + (time * 1000));
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> mutedPlayers.remove(player), time * 20L);
    }

    public void notifyPlayerMuteLifted(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("options.messages.mute.lifted"));
        if (message != null) {
            player.sendMessage(message);
        }
    }

    public static boolean isPlayerMuted(Player player) {
        if (!mutedPlayers.containsKey(player)) {
            return false;
        }
        if (mutedPlayers.get(player) < System.currentTimeMillis()) {
            mutedPlayers.remove(player);
            return false;
        }
        return true;
    }
}
