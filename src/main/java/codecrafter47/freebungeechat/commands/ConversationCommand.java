package codecrafter47.freebungeechat.commands;

import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by florian on 10.02.15.
 */
public class ConversationCommand extends Command {
    private final FreeBungeeChat plugin;

    public ConversationCommand(FreeBungeeChat plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {

    }
}
