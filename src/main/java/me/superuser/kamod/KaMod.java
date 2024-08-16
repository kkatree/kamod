package me.superuser.kamod;

import me.superuser.kamod.listeners.AsyncPlayerChatEvent;
import me.superuser.kamod.utils.BanManager;
import me.superuser.kamod.utils.MuteManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONTokener;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public final class KaMod extends JavaPlugin {

    private MuteManager muteManager;
    private BanManager banManager;
    private File config;
    private File wordsFile;
    private FileConfiguration wordsConfig;
    private Map<String, Long> mutedPlayers = new HashMap<>();
    private Map<String, String> muteReasons = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.createWordsFile();
        muteManager = new MuteManager(this);
        banManager = new BanManager(this);

        getServer().getPluginManager().registerEvents(new AsyncPlayerChatEvent(this), this);
        getLogger().info("Eklenti aktif edildi.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public BanManager getBanManager() {
        return banManager;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public FileConfiguration getWordsConfig() {
        return wordsConfig;
    }

    public Map<String, Long> getMutedPlayers() {
        return mutedPlayers;
    }

    public Map<String, String> getMuteReasons() {
        return muteReasons;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kamod")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.usage.empty")));
                return true;
            }

            if (args[0].equalsIgnoreCase("mute")) {
                if (sender.hasPermission("kamod.mute")) {
                    if (args.length < 4) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.usage.mute")));
                        return true;
                    }
                    String playerName = args[1];
                    long time = parseTime(args[2]);
                    String reason = joinArgs(args, 3);
                    Player target = getServer().getPlayer(playerName);
                    if (target != null) {
                        muteManager.mutePlayer(target, time);
                        mutedPlayers.put(playerName, System.currentTimeMillis() + time * 1000);
                        muteReasons.put(playerName, reason);
                        target.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.mute.mute")), sender.getName(), time, reason, "", ""));
                        sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.mute.staffMute")), target.getName(), time, reason, "", ""));
                        Bukkit.broadcastMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.mute.announceMute")), target.getName(), time, reason, sender.getName(), ""));
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.mute.playerNotFound")));
                    }
                }
            } else if (args[0].equalsIgnoreCase("unmute")) {
                if (sender.hasPermission("kamod.unmute")) {
                    String playerName = args[1];
                    if (playerName.length() < 0) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.usage.unmute")));
                        return false;
                    }
                    Player player = getServer().getPlayer(playerName);
                    if (getMutedPlayers().containsKey(player.getName())) {
                        this.getMutedPlayers().remove(player.getName());
                        this.getMuteReasons().remove(player.getName());
                        sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.messages.mute.unMuted")), player.getName(), 0, "", "", ""));
                        this.getMuteManager().notifyPlayerMuteLifted(player);
                    } else {
                        sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.messages.mute.isNotMuted")), player.getName(), 0, "", "", ""));
                        return false;
                    }

                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("kamod.use")) {
                    reloadConfig();
                    saveConfig();
                    saveWordsConfig();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.reload")));
                } else {
                    return false;
                }

            } else if (args[0].equalsIgnoreCase("küfür")) {
                if (!sender.hasPermission("kamod.use"));
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.usage.wordsManage")));
                    return true;
                }
                String action = args[1];
                String word = args[2];
                if (action.equalsIgnoreCase("ekle")) {
                    List<String> bannedWords = wordsConfig.getStringList("bannedWords");
                    bannedWords.add(word.toLowerCase());
                    wordsConfig.set("bannedWords", bannedWords);
                    saveWordsConfig();
                    sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.messages.wordsManage.added")), "", 0, "", "", word));
                } else if (action.equalsIgnoreCase("çıkar")) {
                    List<String> bannedWords = wordsConfig.getStringList("bannedWords");
                    if (bannedWords.contains(word.toLowerCase())) {
                        bannedWords.remove(word.toLowerCase());
                        wordsConfig.set("bannedWords", bannedWords);
                        saveWordsConfig();
                        sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.messages.wordsManage.removed")), "", 0, "", "", word));
                    } else {
                        sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.messages.wordsManage.notHas")), "", 0, "", "", word));
                    }
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.message.usage.wordsManage")));
                }
            } else if (args[0].equalsIgnoreCase("argo")) {
                if (!sender.hasPermission("kamod.use"));
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.usage.wordsManage")));
                    return true;
                }
                String action = args[1];
                String word = args[2];
                if (action.equalsIgnoreCase("ekle")) {
                    List<String> bannedWords = wordsConfig.getStringList("bannedSlangs");
                    bannedWords.add(word.toLowerCase());
                    wordsConfig.set("bannedSlangs", bannedWords);
                    saveWordsConfig();
                    sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.messages.slangManage.added")), "", 0, "", "", word));
                } else if (action.equalsIgnoreCase("çıkar")) {
                    List<String> bannedWords = wordsConfig.getStringList("bannedSlangs");
                    if (bannedWords.contains(word.toLowerCase())) {
                        bannedWords.remove(word.toLowerCase());
                        wordsConfig.set("bannedSlangs", bannedWords);
                        saveWordsConfig();
                        sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.messages.slangManage.removed")), "", 0, "", "", word));
                    } else {
                        sender.sendMessage(formatMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.messages.slangManage.notHas")), "", 0, "", "", word));
                    }
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.message.usage.slangManage")));
                }
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("options.messages.usage.empty")));
            }
            return true;
        }

        return false;
    }
    public String formatMessage(String message, String player, long time, String reason, String staff, String word) {
        if (message == null) {
            return "Hata oluştu.";
        }
        String formattedTime = getFormattedTime(time);
        return message.replace("%player%", player)
                .replace("%time%", formattedTime)
                .replace("%reason%", reason)
                .replace("%staff%", staff)
                .replace("%word%", word);
    }

    public String formatMessageNew(String message, String player, String time, String reason, String staff, String word) {
        if (message == null) {
            return "Hata oluştu.";
        }
        return message.replace("%player%", player)
                .replace("%time%", time)
                .replace("%reason%", reason)
                .replace("%staff%", staff)
                .replace("%word%", word);
    }

    public String getFormattedTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss", new Locale("tr", "TR"));
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Istanbul"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return sdf.format(calendar.getTime());
    }

    private long parseTime(String time) {
        try {
            return Long.parseLong(time);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                sb.append(" ");
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private String formatTime(long seconds) {
        long millis = seconds * 1000;
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss", new Locale("tr", "TR"));
        return formatter.format(new Date(millis));
    }
    private void createWordsFile() {
        wordsFile = new File(getDataFolder(), "words.yml");
        if (!wordsFile.exists()) {
            wordsFile.getParentFile().mkdirs();
            saveResource("words.yml", false);
        }
        wordsConfig = YamlConfiguration.loadConfiguration(wordsFile);
    }

    private void saveWordsConfig() {
        try {
            wordsConfig.save(wordsFile);
        } catch (IOException e) {
            getLogger().severe("Kayıt edilemedi!");
            e.printStackTrace();
        }
    }
}
