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
    public final Map<String, List<String>> ignoredPlayers = new HashMap<>();
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(final ChatEvent event) {
        // ignore canceled chat
        if (event.isCancelled()) return;

        if (!(event.getSender() instanceof ProxiedPlayer)) return;

        // is this global chat?
        if (!config.getBoolean("alwaysGlobalChat", true)) return;

        if (excludedServers.contains(((ProxiedPlayer) event.getSender()).getServer().getInfo().getName())) return;

        // ignore commands
        if (event.isCommand()) {
            return;
        }

        // cancel event
        event.setCancelled(true);

        getProxy().getScheduler().runAsync(this, new Runnable() {
            @Override
            public void run() {
                try {
                    String message = event.getMessage();

                    message = preparePlayerChat(message, (ProxiedPlayer) event.getSender());
                    message = replaceRegex(message);
                    message = applyTagLogic(message);

                    // replace variables
                    String text = config.getString("chatFormat").replace("%player%",
                            wrapVariable(((ProxiedPlayer) event.getSender()).getDisplayName()));
                    text = text.replace("%message%", message);
                    text = bukkitBridge.replaceVariables(((ProxiedPlayer) event.getSender()), text, "");

                    // broadcast message
                    BaseComponent[] msg = ChatParser.parse(text);
                    for (ProxiedPlayer target : getProxy().getPlayers()) {
                        if (ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(((ProxiedPlayer) event.getSender()).getName()))
                            continue;
                        if (!excludedServers.contains(target.getServer().getInfo().getName())){
                            target.sendMessage(msg);
                        }
                    }
                } catch (Throwable th) {
                    try {
                        ((ProxiedPlayer) event.getSender()).sendMessage(ChatParser.parse("&cAn internal error occurred while processing your chat message."));
                    } catch (Throwable ignored) {
                        // maybe the player is offline?
                    }
                    getLogger().log(Level.SEVERE, "Error while processing chat message", th);
                }
            }
        });
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        String name = event.getPlayer().getName();
        if (replyTarget.containsKey(name)) replyTarget.remove(name);
        if (ignoredPlayers.containsKey(name)) ignoredPlayers.remove(name);
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
        if (commandLine.startsWith("/tell") || commandLine.startsWith("/message") || commandLine.startsWith("/w") || commandLine.startsWith("/whisper") || commandLine.startsWith("/msg")) {
            event.getSuggestions().clear();
            String[] split = commandLine.split(" ");
            String begin = split[split.length - 1];
            for (ProxiedPlayer player : getProxy().getPlayers()) {
                if (player.getName().contains(begin) || player.getDisplayName().contains(begin)) {
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

}
