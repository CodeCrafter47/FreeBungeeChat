package codecrafter47.freebungeechat.commands;

import codecrafter47.freebungeechat.ChatParser;
import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.io.IOException;

/**
 * Created by florian on 28.01.15.
 */
public class ReloadCommand extends Command {
    private final FreeBungeeChat plugin;

    public ReloadCommand(FreeBungeeChat plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (strings.length == 1 && strings[0].equalsIgnoreCase("reload")) {
            try {
                plugin.reloadConfig();
                commandSender.sendMessage(ChatParser.parse("[color=blue][[color=red]FreeBungeeChat[/color]][/color] &aConfiguration has been reloaded."));
            } catch (IOException e) {
                e.printStackTrace();
                commandSender.sendMessage(ChatParser.parse("[color=blue][[color=red]FreeBungeeChat[/color]][/color] &cThere has been an error while reloading the config. See the console for more details"));
            }
        } else {
            commandSender.sendMessage(ChatParser.parse("[color=blue][[color=red]FreeBungeeChat[/color]][/color] &f[suggest]/freebungeechat reload[/suggest]"));
        }
    }
}
