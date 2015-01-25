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

import codecrafter47.freebungeechat.bukkitbridge.Constants;
import com.google.common.base.Charsets;
import lombok.SneakyThrows;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class FreeBungeeChat extends Plugin implements Listener{
    private final Map<String, String> replyTarget = new HashMap<>();
	private final Map<String, List<String>> ignoredPlayers = new HashMap<>();
    Configuration config;
    public static FreeBungeeChat instance;

	List<String> excludedServers = new ArrayList<>();

	BukkitBridge bukkitBridge;

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

		if(config.getStringList("excludeServers") != null){
			excludedServers = config.getStringList("excludeServers");
		}


		getProxy().registerChannel(Constants.channel);
		bukkitBridge = new BukkitBridge(this);
		bukkitBridge.enable();

        super.getProxy().getPluginManager().registerListener(this, this);

        super.getProxy().getPluginManager().registerCommand(this, new Command(
                "whisper", null, "w", "msg", "message", "tell") {

            @Override
            public void execute(CommandSender cs, String[] args) {
                if (args.length < 1) {
                    return;
                }
                ProxiedPlayer target = getProxy().getPlayer(args[0]);
                ProxiedPlayer player = (ProxiedPlayer) cs;
                if (target == null) {
                    String text = config.getString("unknownTarget").replaceAll(
                            "%target%", args[0]);
                    player.sendMessage(ChatParser.parse(text));
                    return;
                }

                String text = "";
                for (int i = 1; i < args.length; i++) {
                    text = text + args[i] + " ";
                }

				// check ignored
				if(ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(player.getName())){
					text = config.getString("ignored").replaceAll(
							"%target%", args[0]);
					player.sendMessage(ChatParser.parse(text));
					return;
				}

				text = replaceRegex(text);

                player.sendMessage(ChatParser.parse(
                        config.getString("privateMessageSend").replaceAll(
                                "%target%", target.
                                        getDisplayName()).replaceAll(
                                "%player%", player.
                                        getDisplayName()).replace(
                                "%message%", Matcher.quoteReplacement(text))));

                target.sendMessage(ChatParser.parse(
                        config.getString("privateMessageReceive").replaceAll(
                                "%target%", target.
                                        getDisplayName()).replaceAll(
                                "%player%", player.
                                        getDisplayName()).replace(
                                "%message%", Matcher.quoteReplacement(text))));

                replyTarget.put(target.getName(), player.getName());
            }

        });

        super.getProxy().getPluginManager().registerCommand(this, new Command(
                "reply", null, "r") {

            @Override
            public void execute(CommandSender cs, String[] args) {

                ProxiedPlayer player = (ProxiedPlayer) cs;

                ProxiedPlayer target = getReplyTarget(player);

                if (target == null) {
                    String text = config.getString("unknownTarget").replaceAll(
                            "%target%",
							args[0]);
                    player.sendMessage(ChatParser.parse(text));
                    return;
                }

				// check ignored
				if(ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(player.getName())){
					String text = config.getString("ignored").replaceAll(
							"%target%",
							args[0]);
					player.sendMessage(ChatParser.parse(text));
					return;
				}

                String text = "";
                for (String arg : args) {
                    text = text + arg + " ";
                }

				text = replaceRegex(text);

                player.sendMessage(ChatParser.parse(
						replaceVariables(target, replaceVariables(player, config.getString("privateMessageSend").replaceAll(
                                "%target%", target.
                                        getDisplayName()).replaceAll(
                                "%player%", player.
                                        getDisplayName()).replace(
                                "%message%", text), ""), "t")));

                target.sendMessage(ChatParser.parse(
						replaceVariables(target, replaceVariables(player, config.getString("privateMessageReceive").replaceAll(
                                "%target%", target.
                                        getDisplayName()).replaceAll(
                                "%player%", player.
                                        getDisplayName()).replace(
								"%message%", Matcher.quoteReplacement(text)), ""), "t")));

                replyTarget.put(target.getName(), player.getName());
            }

        });

        if(!config.getBoolean("alwaysGlobalChat", true)) {
            super.getProxy().getPluginManager().registerCommand(this, new Command(
                    "global", null, "g") {

                @Override
                public void execute(CommandSender cs, String[] args) {
                    if(!(cs instanceof ProxiedPlayer)){
                        cs.sendMessage("Only players can do this");
                        return;
                    }

                    String message = "";
                    for (String arg : args) {
                        message = message + arg + " ";
                    }

					message = replaceRegex(message);

                    // replace variables
                    String text = config.getString("chatFormat").replaceAll("%player%",
							((ProxiedPlayer) cs).getDisplayName());
                    text = text.replaceAll("%message%", Matcher.quoteReplacement(message));
					text = replaceVariables(((ProxiedPlayer) cs), text, "");

                    // broadcast message
                    BaseComponent[] msg = ChatParser.parse(text);
					for(ProxiedPlayer target: getProxy().getPlayers()){
						if(ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(cs.getName()))continue;
						if(target.getServer() == null || !excludedServers.contains(target.getServer().getInfo().getName()))target.sendMessage(msg);
					}
                }
            });
        }

		if(config.getBoolean("enableIgnoreCommand", true)) {
			super.getProxy().getPluginManager().registerCommand(this, new Command(
					"ignore", null) {

				@Override
				public void execute(CommandSender cs, String[] args) {
					if(!(cs instanceof ProxiedPlayer)){
						cs.sendMessage("Only players can do this");
						return;
					}

					if(args.length != 1){
						cs.sendMessage("/ignore <player>");
					}

					ProxiedPlayer toIgnore = getProxy().getPlayer(args[0]);

					if(toIgnore == null){
						String text = config.getString("unknownTarget").replaceAll(
								"%target%",
								args[0]);
						cs.sendMessage(ChatParser.parse(text));
						return;
					}

					// add player to ignore list
					List<String> ignoreList = ignoredPlayers.get(cs.getName());
					if(ignoreList == null) ignoreList = new ArrayList<>(1);
					if(!ignoreList.contains(toIgnore.getName())) {
						ignoreList.add(toIgnore.getName());
						String text = config.getString("ignoreSuccess").replaceAll(
								"%target%",
								args[0]);
						cs.sendMessage(ChatParser.parse(text));
					}
					else {
						ignoreList.remove(toIgnore.getName());
						String text = config.getString("ignoreUnignore").replaceAll(
								"%target%",
								args[0]);
						cs.sendMessage(ChatParser.parse(text));
					}
					ignoredPlayers.put(cs.getName(), ignoreList);
				}
			});
		}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatEvent event) {
        // ignore canceled chat
        if(event.isCancelled())return;

		if(!(event.getSender() instanceof ProxiedPlayer))return;

        // is this global chat?
        if(!config.getBoolean("alwaysGlobalChat", true))return;

		if(excludedServers.contains(((ProxiedPlayer)event.getSender()).getServer().getInfo().getName()))return;

        String message = event.getMessage();

        // ignore commands
        if (event.isCommand()) {
            return;
        }

		message = replaceRegex(message);

        // replace variables
        String text = config.getString("chatFormat").replaceAll("%player%",
				((ProxiedPlayer) event.getSender()).getDisplayName());
        text = text.replaceAll("%message%", Matcher.quoteReplacement(message));
		text = replaceVariables(((ProxiedPlayer) event.getSender()), text, "");

		// broadcast message
		BaseComponent[] msg = ChatParser.parse(text);
		for(ProxiedPlayer target: getProxy().getPlayers()){
			if(ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(((ProxiedPlayer) event.getSender()).getName()))continue;
			if(!excludedServers.contains(target.getServer().getInfo().getName()))target.sendMessage(msg);
		}

        // cancel event
        event.setCancelled(true);
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event){
        String name = event.getPlayer().getName();
        if(replyTarget.containsKey(name))replyTarget.remove(name);
		if(ignoredPlayers.containsKey(name))ignoredPlayers.remove(name);
    }

    private ProxiedPlayer getReplyTarget(ProxiedPlayer player) {
        String t = replyTarget.get(player.getName());
        if (t == null) {
            return player;
        }
		return getProxy().getPlayer(t);
    }

    @SneakyThrows
    private void saveResource(String name){
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), name);

        if (!file.exists()) {
            Files.copy(getResourceAsStream(name), file.toPath());
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event){
        String commandLine = event.getCursor();
        if(commandLine.startsWith("/tell") || commandLine.startsWith("/message") || commandLine.startsWith("/w") || commandLine.startsWith("/whisper") || commandLine.startsWith("/msg")){
            event.getSuggestions().clear();
            String[] split = commandLine.split(" ");
            String begin = split[split.length - 1];
            for(ProxiedPlayer player: getProxy().getPlayers()){
                if(player.getName().contains(begin) || player.getDisplayName().contains(begin)){
                    event.getSuggestions().add(player.getName());
                }
            }
        }
    }

	private String replaceVariables(ProxiedPlayer player, String text, String prefix){
		text = text.replaceAll("%"+prefix+"group%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "group")));
		text = text.replaceAll("%"+prefix+"prefix%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "prefix")));
		text = text.replaceAll("%"+prefix+"suffix%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "suffix")));
		text = text.replaceAll("%"+prefix+"balance%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "balance")));
		text = text.replaceAll("%"+prefix+"currency%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "currency")));
		text = text.replaceAll("%"+prefix+"currencyPl%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "currencyPl")));
		text = text.replaceAll("%"+prefix+"tabName%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "tabName")));
		text = text.replaceAll("%"+prefix+"displayName%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "displayName")));
		text = text.replaceAll("%"+prefix+"world%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "world")));
		text = text.replaceAll("%"+prefix+"health%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "health")));
		text = text.replaceAll("%"+prefix+"level%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "level")));
		text = text.replaceAll("%"+prefix+"server%", Matcher.quoteReplacement(bukkitBridge.getPlayerInformation(player, "server")));
		text = text.replaceAll("%newline%", "\n");
		return text;
	}

	private String replaceRegex(String str){
		List list = config.getList("regex");
		if(list == null)return str;
		for(Object entry: list){
			Map map = (Map) entry;
			str = str.replaceAll(String.valueOf(map.get("search")), String.valueOf(map.get("replace")));
		}
		return str;
	}
}
