package me.superuser.kamod.utils;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Date;

public class BanManager {
    private Plugin plugin;

    public BanManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void banPlayer(Player player, long time, String reason) {
        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason, time > 0 ? new Date(System.currentTimeMillis() + (time * 1000)) : null, null);
        player.kickPlayer(plugin.getConfig().getString("options.messages.ban-message"));
    }
}
