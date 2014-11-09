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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FreeBungeeChat extends Plugin implements Listener{
    private final Map<String, String> replyTarget = new HashMap<>();
	private final Map<String, List<String>> ignoredPlayers = new HashMap<>();
    Configuration config;
    public static FreeBungeeChat instance;

	List<String> excludedServers = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;

        saveResource("config.yml");
        saveResource("LICENSE");
        saveResource("readme.md");

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

		if(config.getStringList("excludeServers") != null){
			excludedServers = config.getStringList("excludeServers");
		}

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
                            "%target%",
                            args[0]);
                    player.sendMessage(ChatUtil.parseString(text));
                    return;
                }

                String text = "";
                for (int i = 1; i < args.length; i++) {
                    text = text + args[i] + " ";
                }

				// check ignored
				if(ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(player.getName())){
					text = config.getString("ignored").replaceAll(
							"%target%",
							args[0]);
					player.sendMessage(ChatUtil.parseString(text));
					return;
				}

                player.sendMessage(ChatUtil.parseString(
                        config.getString("privateMessageSend").replaceAll(
                                "%target%", target.
                                        getDisplayName()).replaceAll(
                                "%player%", player.
                                        getDisplayName()).replace(
                                "%message%", text)));

                target.sendMessage(ChatUtil.parseString(
                        config.getString("privateMessageReceive").replaceAll(
                                "%target%", target.
                                        getDisplayName()).replaceAll(
                                "%player%", player.
                                        getDisplayName()).replace(
                                "%message%", text)));

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
                    player.sendMessage(ChatUtil.parseString(text));
                    return;
                }

				// check ignored
				if(ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(player.getName())){
					String text = config.getString("ignored").replaceAll(
							"%target%",
							args[0]);
					player.sendMessage(ChatUtil.parseString(text));
					return;
				}

                String text = "";
                for (String arg : args) {
                    text = text + arg + " ";
                }

                player.sendMessage(ChatUtil.parseString(
                        config.getString("privateMessageSend").replaceAll(
                                "%target%", target.
                                        getDisplayName()).replaceAll(
                                "%player%", player.
                                        getDisplayName()).replace(
                                "%message%", text)));

                target.sendMessage(ChatUtil.parseString(
                        config.getString("privateMessageReceive").replaceAll(
                                "%target%", target.
                                        getDisplayName()).replaceAll(
                                "%player%", player.
                                        getDisplayName()).replace(
                                "%message%", text)));

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

                    // replace variables
                    String text = config.getString("chatFormat").replaceAll("%player%",
                            ((ProxiedPlayer) cs).getDisplayName());
                    text = text.replaceAll("%message%", message);

                    // broadcast message
                    BaseComponent[] msg = ChatUtil.parseString(text);
					for(ProxiedPlayer target: getProxy().getPlayers()){
						if(ignoredPlayers.get(target.getName()) != null && ignoredPlayers.get(target.getName()).contains(cs.getName()))continue;
						if(!excludedServers.contains(target.getServer().getInfo().getName()))target.sendMessage(msg);
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
						cs.sendMessage(ChatUtil.parseString(text));
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
						cs.sendMessage(ChatUtil.parseString(text));
					}
					else {
						ignoreList.remove(toIgnore.getName());
						String text = config.getString("ignoreUnignore").replaceAll(
								"%target%",
								args[0]);
						cs.sendMessage(ChatUtil.parseString(text));
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
        if (message.matches(" */.*")) {
            return;
        }

        // replace variables
        String text = config.getString("chatFormat").replaceAll("%player%",
                ((ProxiedPlayer) event.getSender()).getDisplayName());
        text = text.replaceAll("%message%", message);

		// broadcast message
		BaseComponent[] msg = ChatUtil.parseString(text);
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
}
