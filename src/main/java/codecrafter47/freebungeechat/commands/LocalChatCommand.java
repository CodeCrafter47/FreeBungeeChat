package codecrafter47.freebungeechat.commands;

import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by florian on 28.01.15.
 */
public class LocalChatCommand extends Command {

    private FreeBungeeChat plugin;

    public LocalChatCommand(FreeBungeeChat plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(final CommandSender cs, final String[] args) {
        if (!(cs instanceof ProxiedPlayer)) {
            cs.sendMessage("Player only command");
            return;
        }

        String message = "";
        for (String arg : args) {
            message = message + arg + " ";
        }

        if (message.isEmpty()) {
            plugin.endConversation((ProxiedPlayer) cs, false);
            return;
        }

        final String finalMessage = message;
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                ((ProxiedPlayer) cs).chat(finalMessage);
            }
        });
    }
}
