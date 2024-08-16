package me.superuser.kamod.listeners;

import me.superuser.kamod.KaMod;
import me.superuser.kamod.utils.MuteManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AsyncPlayerChatEvent implements Listener {
    private KaMod plugin;

    public AsyncPlayerChatEvent(KaMod plugin) {
        this.plugin = plugin;
    }
    @EventHandler @Deprecated
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (plugin.getMutedPlayers().containsKey(player.getName())) {
            long muteEndTime = plugin.getMutedPlayers().get(player.getName());
            long currentTime = System.currentTimeMillis();

            if (currentTime < muteEndTime) {
                event.setCancelled(true);

                long remainingTime = muteEndTime - currentTime;
                String remainTime = formatRemainingTime(remainingTime);
                String reason = plugin.getMuteReasons().get(player.getName());

                String sendMessage = plugin.formatMessageNew(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("options.messages.mute.remainingTime")), player.getName(), remainTime, reason, "", "");
                player.sendMessage(sendMessage);
            } else {
                plugin.getMutedPlayers().remove(player.getName());
                plugin.getMuteReasons().remove(player.getName());

                plugin.getMuteManager().notifyPlayerMuteLifted(player);
            }
            return;
        }

        List<String> bannedWords = plugin.getWordsConfig().getStringList("bannedWords");
        for (String word : bannedWords) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                event.setCancelled(true);
                handleAutomaticMute(player, word);
                return;
            }
        }
    }

    private void handleAutomaticMute(Player player, String word) {
        long muteDuration = plugin.getConfig().getLong("options.time.bannedWordsTime");

        plugin.getMutedPlayers().put(player.getName(), System.currentTimeMillis() + muteDuration * 1000);
        plugin.getMuteReasons().put(player.getName(), "Küfürlü Konuşma");

        String formattedTime = formatRemainingTime(muteDuration * 1000);
        player.sendMessage(plugin.formatMessageNew(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("options.messages.mute.mute")), player.getName(), formattedTime, "Küfürlü Konuşma", "", ""));
        Bukkit.broadcastMessage(plugin.formatMessageNew(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("options.messages.mute.announceMute")), player.getName(), formattedTime, "Küfürlü Konuşma", "Otomatik Susturma", ""));
    }

    private String formatRemainingTime(long remainingTimeMillis) {
        long seconds = remainingTimeMillis / 1000;
        long days = seconds / (24 * 3600);
        seconds = seconds % (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) {
            formattedTime.append(days).append(" gün ");
        }
        if (hours > 0) {
            formattedTime.append(hours).append(" saat ");
        }
        if (minutes > 0) {
            formattedTime.append(minutes).append(" dakika ");
        }
        formattedTime.append(seconds).append(" saniye");

        return formattedTime.toString();
    }
}
