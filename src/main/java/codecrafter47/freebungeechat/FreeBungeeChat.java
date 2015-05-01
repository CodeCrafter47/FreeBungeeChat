/*
 * Copyright (C) 2014 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codecrafter47.freebungeechat;

import codecrafter47.freebungeechat.bukkit.Constants;
import codecrafter47.freebungeechat.commands.*;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FreeBungeeChat extends Plugin implements Listener {
    public final Map<String, String> replyTarget = new HashMap<>();
    public final Map<String, String> persistentConversations = new HashMap<>();
    public final Map<String, List<String>> ignoredPlayers = new HashMap<>();
    public final Map<String, AntiSpamData> spamDataMap = new HashMap<>();
    public Configuration config;
    public static FreeBungeeChat instance;

    public List<String> excludedServers = new ArrayList<>();

    public BukkitBridge bukkitBridge;

    @Override
    public void onEnable() {
        instance = this;

        saveResource("config.yml");
        saveResource("LICENSE");
        saveResource("readme.md");

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new InputStreamReader(new FileInputStream(new File(getDataFolder(), "config.yml")), Charsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (config.getStringList("excludeServers") != null) {
            excludedServers = config.getStringList("excludeServers");
        }

        getProxy().registerChannel(Constants.channel);
        bukkitBridge = new BukkitBridge(this);
        bukkitBridge.enable();

        super.getProxy().getPluginManager().registerListener(this, this);
        List<String> aliases;

        aliases = config.getStringList("adminCommandAliases");
        if (aliases == null || aliases.isEmpty()) aliases = Arrays.asList("freebungeechat", "fbc");
        if (config.getBoolean("enableAdminCommand", true)) {
            super.getProxy().getPluginManager().registerCommand(this,
                    new ReloadCommand(this, aliases.get(0), "freebungeechat.admin",
                            aliases.subList(1, aliases.size()).toArray(new String[aliases.size() - 1])));
        }

        aliases = config.getStringList("messageCommandAliases");
        if (aliases == null || aliases.isEmpty()) aliases = Arrays.asList("w", "msg", "message", "tell", "whisper");
        if (config.getBoolean("enableMessageCommand", true)) {
            super.getProxy().getPluginManager().registerCommand(this, new MessageCommand(this, aliases.get(0), null,
                    aliases.subList(1, aliases.size()).toArray(new String[aliases.size() - 1])));
        }

        aliases = config.getStringList("replyCommandAliases");
        if (aliases == null || aliases.isEmpty()) aliases = Arrays.asList("reply", "r");
        if (config.getBoolean("enableReplyCommand", true)) {
            super.getProxy().getPluginManager().registerCommand(this, new ReplyCommand(this, aliases.get(0), null,
                    aliases.subList(1, aliases.size()).toArray(new String[aliases.size() - 1])));
        }

        aliases = config.getStringList("globalChatCommandAliases");
        if (aliases == null || aliases.isEmpty()) aliases = Arrays.asList("global", "g");
        if (config.getBoolean("enableGlobalChatCommand", true)) {
            super.getProxy().getPluginManager().registerCommand(this, new GlobalChatCommand(this, aliases.get(0), null,
                    aliases.subList(1, aliases.size()).toArray(new String[aliases.size() - 1])));
        }

        aliases = config.getStringList("ignoreCommandAliases");
        if (aliases == null || aliases.isEmpty()) aliases = Arrays.asList("ignore");
        if (config.getBoolean("enableIgnoreCommand", true)) {
            super.getProxy().getPluginManager().registerCommand(this, new IgnoreCommand(this, aliases.get(0), null,
                    aliases.subList(1, aliases.size()).toArray(new String[aliases.size() - 1])));
        }

        aliases = config.getStringList("conversationCommandAliases");
        if (aliases == null || aliases.isEmpty()) aliases = Arrays.asList("chat");
        if (config.getBoolean("enableConversationCommand", true)) {
            super.getProxy().getPluginManager().registerCommand(this, new ConversationCommand(this, aliases.get(0), null,
                    aliases.subList(1, aliases.size()).toArray(new String[aliases.size() - 1])));
        }
    }

    /**
     * Checks whether a player is spamming
     *
     * @param player the player
     * @return true if chat should be cancelled
     */
    public boolean checkSpam(ProxiedPlayer player) {
        if (!config.getBoolean("enableAntiSpam", true))
            return false;
        String name = player.getName();
        if (!spamDataMap.containsKey(name)) {
            spamDataMap.put(name, new AntiSpamData());
        }
        AntiSpamData antiSpamData = spamDataMap.get(name);
        if (antiSpamData.isSpamming()) {
            player.sendMessage(ChatParser.parse(config.getString("antiSpamText",
                    "&cYou send to many messages. Please wait a minute before sending messages again.")));
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(final ChatEvent event) {
        // ignore canceled chat
        if (event.isCancelled()) return;

        if (!(event.getSender() instanceof ProxiedPlayer)) return;

        // ignore commands
        if (event.isCommand()) {
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        if (persistentConversations.containsKey(player.getName())) {
            final ProxiedPlayer target = getProxy().getPlayer(persistentConversations.get(player.getName()));
            if (target != null) {
                getProxy().getScheduler().runAsync(this, new Runnable() {
                    @Override
                    public void run() {
                        sendPrivateMessage(event.getMessage(), target, player);
                    }
                });
                event.setCancelled(true);
                return;
            } else {
                player.sendMessage(ChatParser.parse(config.getString("unknownTarget").replace(
                        "%target%", wrapVariable(persistentConversations.get(player.getName())))));
                endConversation(player, true);
            }
        }

        // is this global chat?
        if (!config.getBoolean("alwaysGlobalChat", true)) return;

        if (excludedServers.contains(player.getServer().getInfo().getName())) return;

        // cancel event
        event.setCancelled(true);

        final String message = event.getMessage();

        getProxy().getScheduler().runAsync(this, new Runnable() {
            @Override
            public void run() {
                sendGlobalChatMessage(player, message);
            }
        });
    }

    public void endConversation(ProxiedPlayer player, boolean force) {
        if (force || persistentConversations.containsKey(player.getName())) {
            if(persistentConversations.containsKey(player.getName())) {
                player.sendMessage(ChatParser.parse(config.getString("endConversation").replace(
                        "%target%", wrapVariable(persistentConversations.get(player.getName())))));
                persistentConversations.remove(player.getName());
            } else {
                player.sendMessage(ChatParser.parse(config.getString("endConversation").replace(
                        "%target%", "nobody")));
            }
        }
    }

    public void sendGlobalChatMessage(ProxiedPlayer player, String message) {
        try {
            if (checkSpam(player)) {
                return;
            }
            message = preparePlayerChat(message, player);
            message = replaceRegex(message);
            message = applyTagLogic(message);

            // replace variables
            String text = config.getString("chatFormat").replace("%player%",
                    wrapVariable(player.getDisplayName()));
            text = text.replace("%message%", message);
            text = bukkitBridge.replaceVariables(player, text, "");

            // broadcast message
            BaseComponent[] msg = ChatParser.parse(text);
            for (ProxiedPlayer target : getProxy().getPlayers()) {
                if (ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(player.getName()))
                    continue;
                if (!excludedServers.contains(target.getServer().getInfo().getName())) {
                    target.sendMessage(msg);
                }
            }
            if(config.getBoolean("logChat", false)){
                getLogger().info(player.getName() + ": " + message);
            }
        } catch (Throwable th) {
            try {
                player.sendMessage(ChatParser.parse("&cAn internal error occurred while processing your chat message."));
            } catch (Throwable ignored) {
                // maybe the player is offline?
            }
            getLogger().log(Level.SEVERE, "Error while processing chat message", th);
        }
    }

    public void sendGlobalConsoleChatMessage(String message) {
        try {
            message = replaceRegex(message);
            message = applyTagLogic(message);

            // replace variables
            String text = config.getString("chatFormat").replace("%player%",
                    config.getString("consoleName", "SERVER"));
            text = text.replace("%message%", message);
            text = text.replaceAll("%(group|prefix|suffix|balance|currency|currencyPl|tabName|displayName|world|health|level)%", "");

            // broadcast message
            BaseComponent[] msg = ChatParser.parse(text);
            for (ProxiedPlayer target : getProxy().getPlayers()) {
                if (!excludedServers.contains(target.getServer().getInfo().getName())) {
                    target.sendMessage(msg);
                }
            }
            if(config.getBoolean("logChat", false)){
                getLogger().info(config.getString("consoleName", "SERVER") + ": " + message);
            }
        } catch (Throwable th) {
            getLogger().log(Level.SEVERE, "Error while processing chat message", th);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        String name = event.getPlayer().getName();
        if (replyTarget.containsKey(name)) replyTarget.remove(name);
        if (ignoredPlayers.containsKey(name)) ignoredPlayers.remove(name);
        if (persistentConversations.containsKey(name)) persistentConversations.remove(name);
        if (spamDataMap.containsKey(name)) spamDataMap.remove(name);
    }

    public ProxiedPlayer getReplyTarget(ProxiedPlayer player) {
        String t = replyTarget.get(player.getName());
        if (t == null) {
            return player;
        }
        return getProxy().getPlayer(t);
    }

    @SneakyThrows
    private void saveResource(String name) {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), name);

        if (!file.exists()) {
            Files.copy(getResourceAsStream(name), file.toPath());
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String commandLine = event.getCursor();
        if (!commandLine.startsWith("/")) return;
        if (commandLine.matches("^/(?:" + Joiner.on('|').join(Iterables.concat(config.getStringList("messageCommandAliases"),
                config.getStringList("conversationCommandAliases"))) + ").*$")) {
            event.getSuggestions().clear();
            String[] split = commandLine.split(" ");
            String begin = split[split.length - 1];
            for (ProxiedPlayer player : getProxy().getPlayers()) {
                if (player.getName().toLowerCase().contains(begin.toLowerCase()) || player.getDisplayName().toLowerCase().contains(begin.toLowerCase())) {
                    event.getSuggestions().add(player.getName());
                }
            }
        }
    }

    public String replaceRegex(String str) {
        List list = config.getList("regex");
        if (list == null) return str;
        for (Object entry : list) {
            Map map = (Map) entry;
            str = str.replaceAll(String.valueOf(map.get("search")), String.valueOf(map.get("replace")));
        }
        return str;
    }

    public String wrapVariable(String variable) {
        if (config.getBoolean("allowBBCodeInVariables", false)) {
            return variable;
        } else {
            return "[nobbcode]" + variable + "[/nobbcode]";
        }
    }

    public String preparePlayerChat(String text, ProxiedPlayer player) {
        if (!player.hasPermission("freebungeechat.chat.color")) {
            text = ChatColor.translateAlternateColorCodes('&', text);
            text = ChatColor.stripColor(text);
        }
        if (!player.hasPermission("freebungeechat.chat.bbcode")) {
            text = ChatParser.stripBBCode(text);
        }
        return text;
    }

    public void reloadConfig() throws FileNotFoundException {
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new InputStreamReader(new FileInputStream(new File(getDataFolder(), "config.yml")), Charsets.UTF_8));
        if (config.getStringList("excludeServers") != null) {
            excludedServers = config.getStringList("excludeServers");
        }
    }

    public String applyTagLogic(String text) {
        if (!config.getBoolean("enableTaggingPlayers", true)) return text;
        Matcher matcher = Pattern.compile("@(?<name>[^ ]{1,16})").matcher(text);
        StringBuffer stringBuffer = new StringBuffer(text.length());
        while (matcher.find()) {
            String name = matcher.group("name");
            ProxiedPlayer taggedPlayer = getProxy().getPlayer(name);
            if (taggedPlayer != null) {
                matcher.appendReplacement(stringBuffer, config.getString("taggedPlayer", "[suggest=/w ${name}]@${name}[/suggest]"));
                if (config.getBoolean("playSoundToTaggedPlayer", true)) {
                    bukkitBridge.playSound(taggedPlayer, config.getString("playerTaggedSound", "ORB_PICKUP"));
                }
            } else {
                matcher.appendReplacement(stringBuffer, "$0");
            }
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    public void sendPrivateMessage(String text, ProxiedPlayer target, ProxiedPlayer player) {
        if (checkSpam(player)) {
            return;
        }
        // check ignored
        if (ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(player.getName())) {
            text = config.getString("ignored").replace(
                    "%target%", wrapVariable(target.getName()));
            player.sendMessage(ChatParser.parse(text));
            return;
        }

        text = preparePlayerChat(text, player);
        text = replaceRegex(text);

        player.sendMessage(ChatParser.parse(
                bukkitBridge.replaceVariables(target, bukkitBridge.replaceVariables(player, config.getString("privateMessageSend").replace(
                        "%target%", wrapVariable(target.
                                getDisplayName())).replace(
                        "%player%", wrapVariable(player.
                                getDisplayName())).replace(
                        "%message%", text), ""), "t")));

        target.sendMessage(ChatParser.parse(
                bukkitBridge.replaceVariables(target, bukkitBridge.replaceVariables(player, config.getString("privateMessageReceive").replace(
                        "%target%", wrapVariable(target.
                                getDisplayName())).replace(
                        "%player%", wrapVariable(player.
                                getDisplayName())).replace(
                        "%message%", text), ""), "t")));

        replyTarget.put(target.getName(), player.getName());

        if (config.getBoolean("playSoundPrivateMessage", true)) {
            bukkitBridge.playSound(target, config.getString("privateMessageMessageSound", "ORB_PICKUP"));
        }
    }

    public void startConversation(ProxiedPlayer player, ProxiedPlayer target) {
        persistentConversations.put(player.getName(), target.getName());
        player.sendMessage(ChatParser.parse(config.getString("startConversation").replace(
                "%target%", wrapVariable(target.getName()))));
    }
}
