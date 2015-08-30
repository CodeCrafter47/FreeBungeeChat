package codecrafter47.freebungeechat.tasks.bungee;

import codecrafter47.freebungeechat.FreeBungeeChat;
import codecrafter47.freebungeechat.tasks.BungeeTask;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class PutChatMessageInBufferTask extends BungeeTask {
    private final String text;
    private final int id;

    public PutChatMessageInBufferTask(String text, int id) {
        this.text = text;
        this.id = id;
    }

    @Override
    public void execute(FreeBungeeChat plugin, ProxiedPlayer player, Server server) {
        plugin.bukkitBridge.buf.put(id, text);
    }
}
