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
import lombok.SneakyThrows;
import lombok.Synchronized;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Florian Stober
 */
public class BukkitBridge implements Listener {

    FreeBungeeChat plugin;

    ConcurrentHashMap<Integer, String> buf = new ConcurrentHashMap<>();

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

                    DataInputStream in = new DataInputStream(
                            new ByteArrayInputStream(event.getData()));

                    String subchannel = in.readUTF();

                    if (subchannel.equals(Constants.subchannel_chatMsg)) {
                        buf.put(in.readInt(), in.readUTF());
                    }

                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Exception while parsing data from Bukkit", ex);
                }
            }
        }
    }

    public void playSound(ProxiedPlayer player, String sound) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream outputStream1 = new DataOutputStream(outputStream);
            outputStream1.writeUTF(Constants.subchannel_playSound);
            outputStream1.writeUTF(sound);
            outputStream1.flush();
            outputStream1.close();
            player.getServer().sendData(Constants.channel, outputStream.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(BukkitBridge.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    @SneakyThrows
    public String replaceVariables(ProxiedPlayer player, String text, String prefix) {
        int tries = 0;
        while (text.matches("^.*%" + prefix + "(group|prefix(color)?|suffix|balance|currency|currencyPl|tabName|displayName|world|health|level)%.*$")
                && tries < 3) {
            try {
                int id = getId();
                if (buf.containsKey(id)) buf.remove(id);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                DataOutputStream outputStream1 = new DataOutputStream(outputStream);
                outputStream1.writeUTF(Constants.subchannel_chatMsg);
                outputStream1.writeUTF(text);
                outputStream1.writeUTF(prefix);
                outputStream1.writeInt(id);
                outputStream1.writeBoolean(plugin.config.getBoolean("allowBBCodeInVariables", false));
                outputStream1.flush();
                outputStream1.close();
                player.getServer().sendData(Constants.channel, outputStream.toByteArray());
                for (int i = 0; i < 10 && !buf.containsKey(id); i++) {
                    Thread.sleep(100);
                }
                if (buf.containsKey(id)) {
                    text = buf.get(id);
                    buf.remove(id);
                    tries = 0;
                    break;
                }
            } catch (Throwable th) {
                th.printStackTrace();
                Thread.sleep(1000);
            }
            tries++;
        }
        if (tries > 0) {
            throw new RuntimeException("Unable to process chat message from " + player.getName() + " make sure you have installed FreeBungeeChat on " + (player.getServer() != null ? player.getServer().getInfo().getName() : "(unknown server)"));
        }
        text = text.replace("%" + prefix + "server%", plugin.wrapVariable(player.getServer() != null ? player.getServer().getInfo().getName() : "unknown"));
        return text;
    }

    @Synchronized
    private int getId() {
        return cnt++;
    }
}
