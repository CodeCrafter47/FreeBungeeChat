package codecrafter47.freebungeechat.commands;

import codecrafter47.util.chat.ChatParser;
import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by florian on 28.01.15.
 */
public class IgnoreCommand extends Command {

    private final FreeBungeeChat plugin;
    private final ChatParser chatParser;

    public IgnoreCommand(FreeBungeeChat plugin, String name, String permission, ChatParser chatParser, String... aliases) {
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

        if (args.length != 1) {
            cs.sendMessage("/ignore <player>");
        }

        ProxiedPlayer toIgnore = plugin.getProxy().getPlayer(args[0]);

        if (toIgnore == null) {
            String text = plugin.config.getString("unknownTarget").replace(
                    "%target%",
                    plugin.wrapVariable(args[0]));
            cs.sendMessage(chatParser.parse(text));
            return;
        }

        // add player to ignore list
        List<String> ignoreList = plugin.ignoredPlayers.get(cs.getName());
        if (ignoreList == null) ignoreList = new ArrayList<>(1);
        if (!ignoreList.contains(toIgnore.getName())) {
            ignoreList.add(toIgnore.getName());
            String text = plugin.config.getString("ignoreSuccess").replace(
                    "%target%",
                    plugin.wrapVariable(args[0]));
            cs.sendMessage(chatParser.parse(text));
        } else {
            ignoreList.remove(toIgnore.getName());
            String text = plugin.config.getString("ignoreUnignore").replace(
                    "%target%",
                    plugin.wrapVariable(args[0]));
            cs.sendMessage(chatParser.parse(text));
        }
        plugin.ignoredPlayers.put(cs.getName(), ignoreList);
    }
}
