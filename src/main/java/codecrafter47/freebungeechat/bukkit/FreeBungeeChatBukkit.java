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
package codecrafter47.freebungeechat.bukkit;

import codecrafter47.data.Value;
import codecrafter47.data.Values;
import codecrafter47.data.bukkit.PlayerDataAggregator;
import codecrafter47.data.bukkit.ServerDataAggregator;
import codecrafter47.freebungeechat.tasks.BukkitTask;
import codecrafter47.freebungeechat.tasks.BungeeTask;
import codecrafter47.freebungeechat.tasks.bungee.InitServerConnectionTask;
import codecrafter47.freebungeechat.tasks.bungee.PutChatMessageInBufferTask;
import lombok.SneakyThrows;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Florian Stober
 */
public class FreeBungeeChatBukkit extends JavaPlugin implements Listener {
    private List<Variable> variables;

    @Override
    public void onEnable() {
        registerVariables();
        getServer().getMessenger().registerOutgoingPluginChannel(this,
                Constants.channel);
        getServer().getMessenger().registerIncomingPluginChannel(this,
                Constants.channel, new PluginMessageListener() {

                    @Override
                    @SneakyThrows
                    public void onPluginMessageReceived(String string,
                                                        Player player, byte[] bytes) {
                        try {
                            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
                            Object object = in.readObject();
                            Class c = Class.forName((String) object);
                            Constructor[] constructors = c.getConstructors();
                            if (constructors.length == 1) {
                                Object[] args = (Object[]) in.readObject();
                                BukkitTask task = (BukkitTask) constructors[0].newInstance(args);
                                task.execute(FreeBungeeChatBukkit.this, player);
                            } else {
                                getLogger().severe("received invalid task from bungee: " + c);
                            }
                        } catch (Throwable th) {
                            getLogger().log(Level.SEVERE, "failed to parse data from bungee", th);
                        }
                    }
                });
        getServer().getPluginManager().registerEvents(this, this);

        initPlayerConnections();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        registerVariables();
        initPlayerConnections();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        registerVariables();
        initPlayerConnections();
    }

    private void registerVariables(){
        List<Variable> newVariables = new ArrayList<>();

        final PlayerDataAggregator playerData = new PlayerDataAggregator(getLogger());
        Set<? extends Value<?>> availablePlayerData = playerData.getAvailableValueTypes();
        if(availablePlayerData.contains(Values.Player.Vault.Prefix)){
            newVariables.add(new Variable("prefix") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Vault.Prefix, player).orElse("");
                }
            });
            newVariables.add(new Variable("prefixcolor") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Vault.Prefix, player)
                            .filter(s -> s.length() >= 2)
                            .filter(s -> s.startsWith("" + ChatColor.COLOR_CHAR))
                            .map(s -> s.substring(0, 2)).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Vault.Suffix)){
            newVariables.add(new Variable("suffix") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Vault.Suffix, player).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Vault.PermissionGroup)){
            newVariables.add(new Variable("group") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Vault.PermissionGroup, player).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Vault.Balance)){
            newVariables.add(new Variable("balance") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Vault.Balance, player).map(b -> String.format("%1.2f", b)).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Bukkit.PlayerListName)){
            newVariables.add(new Variable("tabName") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Bukkit.PlayerListName, player).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Bukkit.DisplayName)){
            newVariables.add(new Variable("displayName") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Bukkit.DisplayName, player).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Bukkit.World)){
            newVariables.add(new Variable("world") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Bukkit.World, player).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Minecraft.Health)){
            newVariables.add(new Variable("health") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Minecraft.Health, player).map(h -> String.format("%1.1f", h)).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Minecraft.Level)){
            newVariables.add(new Variable("level") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Minecraft.Level, player).map(i -> Integer.toString(i)).orElse("");
                }
            });
        }
        if(availablePlayerData.contains(Values.Player.Factions.FactionName)){
            newVariables.add(new Variable("faction") {
                @Override
                public String getReplacement(Player player) {
                    return playerData.getValue(Values.Player.Factions.FactionName, player).orElse("");
                }
            });
        }

        ServerDataAggregator serverData = new ServerDataAggregator(this);
        Set<? extends Value<?>> availableServerData = serverData.getAvailableValueTypes();
        if(availableServerData.contains(Values.Server.Vault.CurrencyNameSingular)){
            newVariables.add(new Variable("currency") {
                @Override
                public String getReplacement(Player player) {
                    return serverData.getValue(Values.Server.Vault.CurrencyNameSingular, getServer()).orElse("");
                }
            });
        }
        if(availableServerData.contains(Values.Server.Vault.CurrencyNamePlural)){
            newVariables.add(new Variable("currencyPl") {
                @Override
                public String getReplacement(Player player) {
                    return serverData.getValue(Values.Server.Vault.CurrencyNamePlural, getServer()).orElse("");
                }
            });
        }

        variables = newVariables;
    }

    private void initPlayerConnections(){
        for (Player player : getServer().getOnlinePlayers()) {
            initPlayerConnection(player);
        }
    }

    private void initPlayerConnection(Player player) {
        // TODO
        executeOnBungee(player, InitServerConnectionTask.class);
    }

    public void executeOnBungee(Player player, Class<? extends BungeeTask> task, Object... args){
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(task.getName());
            os.writeObject(args);
            os.flush();
            os.close();
            player.sendPluginMessage(this, Constants.channel, out.toByteArray());
        } catch (Throwable th){
            getLogger().log(Level.SEVERE, "Failed to execute task " + task + " for player " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        initPlayerConnection(event.getPlayer());
    }

    @SneakyThrows
    public void processChatMessage(Player player, String text, String prefix, int id, boolean allowBBCode) {
        for (Variable variable : variables) {
            Matcher matcher = Pattern.compile(String.format("%%%s%s%%", prefix, variable.getName())).matcher(text);
            StringBuffer stringBuffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(wrapVariable(variable.getReplacement(player), allowBBCode)));
            }
            matcher.appendTail(stringBuffer);
            text = stringBuffer.toString();
        }

        executeOnBungee(player, PutChatMessageInBufferTask.class, text, id);
    }

    public String wrapVariable(String variable, boolean allowBBCode) {
        if (allowBBCode) {
            return variable;
        } else {
            return "[nobbcode]" + variable + "[/nobbcode]";
        }
    }
}
