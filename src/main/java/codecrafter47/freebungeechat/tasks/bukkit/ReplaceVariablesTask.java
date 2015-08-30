package codecrafter47.freebungeechat.tasks.bukkit;

import codecrafter47.freebungeechat.bukkit.FreeBungeeChatBukkit;
import codecrafter47.freebungeechat.tasks.BukkitTask;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ReplaceVariablesTask extends BukkitTask {
    private final String text;
    private final String prefix;
    private final int id;
    private final boolean allowBBCode;

    public ReplaceVariablesTask(String text, String prefix, int id, boolean allowBBCode) {
        this.text = text;
        this.prefix = prefix;
        this.id = id;
        this.allowBBCode = allowBBCode;
    }

    @Override
    public void execute(FreeBungeeChatBukkit plugin, Player player) {
        plugin.processChatMessage(player, text, prefix, id, allowBBCode);
    }
}
