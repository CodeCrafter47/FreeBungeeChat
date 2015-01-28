package codecrafter47.freebungeechat.commands;

import codecrafter47.freebungeechat.ChatParser;
import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
* Created by florian on 28.01.15.
*/
public class MessageCommand extends Command {

    private FreeBungeeChat plugin;

    public MessageCommand(FreeBungeeChat plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender cs, String[] args) {
        if (args.length < 1) {
            return;
        }
        ProxiedPlayer target = plugin.getProxy().getPlayer(args[0]);
        ProxiedPlayer player = (ProxiedPlayer) cs;
        if (target == null) {
            String text = plugin.config.getString("unknownTarget").replace(
                    "%target%", plugin.wrapVariable(args[0]));
            player.sendMessage(ChatParser.parse(text));
            return;
        }

        String text = "";
        for (int i = 1; i < args.length; i++) {
            text = text + args[i] + " ";
        }

        // check ignored
        if(plugin.ignoredPlayers.get(target.getName()) != null && plugin.ignoredPlayers.get(target.getName()).contains(player.getName())){
            text = plugin.config.getString("ignored").replace(
                    "%target%", plugin.wrapVariable(args[0]));
            player.sendMessage(ChatParser.parse(text));
            return;
        }

        text = plugin.preparePlayerChat(text, player);
        text = plugin.replaceRegex(text);

        player.sendMessage(ChatParser.parse(
                plugin.config.getString("privateMessageSend").replace(
                        "%target%", plugin.wrapVariable(target.
                                getDisplayName())).replace(
                        "%player%", plugin.wrapVariable(player.
                                getDisplayName())).replace(
                        "%message%", text)));

        target.sendMessage(ChatParser.parse(
                plugin.config.getString("privateMessageReceive").replace(
                        "%target%", plugin.wrapVariable(target.
                                getDisplayName())).replace(
                        "%player%", plugin.wrapVariable(player.
                                getDisplayName())).replace(
                        "%message%", text)));

        plugin.replyTarget.put(target.getName(), player.getName());
    }

}
