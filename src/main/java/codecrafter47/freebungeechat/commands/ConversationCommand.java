package codecrafter47.freebungeechat.commands;

import codecrafter47.util.chat.ChatParser;
import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by florian on 10.02.15.
 */
public class ConversationCommand extends Command {
    private final FreeBungeeChat plugin;
    private final ChatParser chatParser;

    public ConversationCommand(FreeBungeeChat plugin, String name, String permission, ChatParser chatParser, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
        this.chatParser = chatParser;
    }

    @Override
    public void execute(CommandSender cs, String[] args) {
        if (!(cs instanceof ProxiedPlayer)) {
            cs.sendMessage("Only players can do this");
            return;
        }
        if (args.length < 1) {
            plugin.endConversation((ProxiedPlayer) cs, true);
            return;
        }
        final ProxiedPlayer target = plugin.getProxy().getPlayer(args[0]);
        final ProxiedPlayer player = (ProxiedPlayer) cs;
        if (target == null) {
            String text = plugin.config.getString("unknownTarget").replace(
                    "%target%", plugin.wrapVariable(args[0]));
            player.sendMessage(chatParser.parse(text));
            return;
        }

        plugin.startConversation(player, target);
    }
}
