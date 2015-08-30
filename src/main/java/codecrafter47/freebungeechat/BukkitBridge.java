/*
 * BungeeTabListPlus - a bungeecord plugin to customize the tablist
 *
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
import codecrafter47.freebungeechat.bukkit.FreeBungeeChatBukkit;
import codecrafter47.freebungeechat.tasks.BukkitTask;
import codecrafter47.freebungeechat.tasks.BungeeTask;
import codecrafter47.freebungeechat.tasks.bukkit.PlaySoundTask;
import codecrafter47.freebungeechat.tasks.bukkit.ReplaceVariablesTask;
import lombok.SneakyThrows;
import lombok.Synchronized;
import net.alpenblock.bungeeperms.BungeePerms;
import net.alpenblock.bungeeperms.Group;
import net.alpenblock.bungeeperms.PermissionsManager;
import net.alpenblock.bungeeperms.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author Florian Stober
 */
public class BukkitBridge implements Listener {

    FreeBungeeChat plugin;

    public ConcurrentHashMap<Integer, String> buf = new ConcurrentHashMap<>();

    int cnt = 0;

    public BukkitBridge(FreeBungeeChat plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getTag().equals(Constants.channel)) {
            event.setCancelled(true);
            if (event.getReceiver() instanceof ProxiedPlayer && event.
                    getSender() instanceof Server) {
                try {
                    ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
                    Server server = (Server) event.getSender();

                    ObjectInputStream in = new ObjectInputStream(
                            new ByteArrayInputStream(event.getData()));
                    Object object = in.readObject();
                    Class c = Class.forName((String) object);
                    Constructor[] constructors = c.getConstructors();
                    if (constructors.length == 1) {
                        Object[] args = (Object[]) in.readObject();
                        BungeeTask task = (BungeeTask) constructors[0].newInstance(args);
                        task.execute(plugin, player, server);
                    } else {
                        plugin.getLogger().severe("received invalid task from bukkit server (" + server.getInfo().getName() + "): " + c);
                    }

                } catch (Throwable th) {
                    plugin.getLogger().log(Level.SEVERE, "Exception while parsing data from Bukkit", th);
                }
            }
        }
    }

    public void playSound(ProxiedPlayer player, String sound) {
        executeOnBukkit(player, PlaySoundTask.class, sound);
    }

    public void executeOnBukkit(ProxiedPlayer player, Class<? extends BukkitTask> task, Object... args){
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(task.getName());
            os.writeObject(args);
            os.flush();
            os.close();
            player.getServer().sendData(Constants.channel, out.toByteArray());
        } catch (Throwable th){
            plugin.getLogger().log(Level.SEVERE, "Failed to execute task " + task + " for player " + player.getName());
        }
    }

    @SneakyThrows
    public String replaceVariables(ProxiedPlayer player, String text, String prefix) {
        if (text.matches("^.*%" + prefix + "(group|prefix(color)?|suffix|balance|currency|currencyPl|tabName|displayName|world|health|level|faction)%.*$")) {
            try {
                int id = getId();
                if (buf.containsKey(id)) buf.remove(id);

                executeOnBukkit(player, ReplaceVariablesTask.class, text, prefix, id, plugin.config.getBoolean("allowBBCodeInVariables", false));
                for (int i = 0; i < 600 && !buf.containsKey(id); i++) {
                    Thread.sleep(100);
                }
                if (buf.containsKey(id)) {
                    text = buf.get(id);
                    buf.remove(id);
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        text = text.replace("%" + prefix + "server%", plugin.wrapVariable(player.getServer() != null ? player.getServer().getInfo().getName() : "unknown"));
        text = text.replace("%" + prefix + "BungeePerms_Prefix%", plugin.wrapVariable(getBungeePermsPrefix(player)));
        text = text.replace("%" + prefix + "BungeePerms_Suffix%", plugin.wrapVariable(getBungeePermsSuffix(player)));
        text = text.replace("%" + prefix + "BungeePerms_Group%", plugin.wrapVariable(getBungeePermsGroup(player)));
        return text;
    }

    @Synchronized
    private int getId() {
        return cnt++;
    }

    private boolean isBungeePermsAvailable() {
        return plugin.getProxy().getPluginManager().getPlugin("BungeePerms") != null;
    }

    private String getBungeePermsPrefix(ProxiedPlayer player) {
        if (isBungeePermsAvailable()) {
            BungeePerms bungeePerms = BungeePerms.getInstance();
            if (bungeePerms != null) {
                PermissionsManager pm = bungeePerms.getPermissionsManager();
                if (pm != null) {
                    User user = pm.getUser(player.getName());
                    if (user != null) {
                        return user.buildPrefix();
                    }
                }
            }
        }
        return "";
    }

    private String getBungeePermsSuffix(ProxiedPlayer player) {
        if (isBungeePermsAvailable()) {
            BungeePerms bungeePerms = BungeePerms.getInstance();
            if (bungeePerms != null) {
                PermissionsManager pm = bungeePerms.getPermissionsManager();
                if (pm != null) {
                    User user = pm.getUser(player.getName());
                    if (user != null) {
                        return user.buildSuffix();
                    }
                }
            }
        }
        return "";
    }

    private String getBungeePermsGroup(ProxiedPlayer player) {
        if (isBungeePermsAvailable()) {
            BungeePerms bungeePerms = BungeePerms.getInstance();
            if (bungeePerms != null) {
                PermissionsManager pm = bungeePerms.getPermissionsManager();
                if (pm != null) {
                    User user = pm.getUser(player.getName());
                    if (user != null) {
                        Group mainGroup = pm.getMainGroup(user);
                        if (mainGroup != null) {
                            return mainGroup.getName();
                        }
                    }
                }
            }
        }
        return "";
    }
}
