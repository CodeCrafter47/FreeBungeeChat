package codecrafter47.freebungeechat.tasks;

import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.io.Serializable;

public abstract class BungeeTask implements Serializable {

    public abstract void execute(FreeBungeeChat plugin, ProxiedPlayer player, Server server);
}
