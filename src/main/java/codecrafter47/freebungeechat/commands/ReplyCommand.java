package codecrafter47.freebungeechat.commands;

import codecrafter47.freebungeechat.ChatParser;
import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by florian on 28.01.15.
 */
public class ReplyCommand extends Command {

    private FreeBungeeChat plugin;

    public ReplyCommand(FreeBungeeChat plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender cs, final String[] args) {
        if(!(cs instanceof ProxiedPlayer)){
            cs.sendMessage("Only players can do this");
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) cs;

        final ProxiedPlayer target = plugin.getReplyTarget(player);

        if (target == null) {
            String text = plugin.config.getString("unknownTarget").replace(
                    "%target%",
                    plugin.wrapVariable(args[0]));
            player.sendMessage(ChatParser.parse(text));
            return;
        }
        String text = "";
        for (String arg : args) {
            text = text + arg + " ";
        }

        final String finalText = text;
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.sendPrivateMessage(finalText, target, player);
            }
        });
    }

}
