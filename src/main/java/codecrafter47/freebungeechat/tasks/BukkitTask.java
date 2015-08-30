package codecrafter47.freebungeechat.tasks;

import codecrafter47.freebungeechat.bukkit.FreeBungeeChatBukkit;
import org.bukkit.entity.Player;

import java.io.Serializable;

public abstract class BukkitTask implements Serializable {

    public abstract void execute(FreeBungeeChatBukkit plugin, Player player);
}
