package codecrafter47.freebungeechat.commands;

import codecrafter47.freebungeechat.ChatParser;
import codecrafter47.freebungeechat.FreeBungeeChat;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
* Created by florian on 28.01.15.
*/
public class GlobalChatCommand extends Command {

    private FreeBungeeChat plugin;

    public GlobalChatCommand(FreeBungeeChat plugin, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(final CommandSender cs, final String[] args) {
        if(!(cs instanceof ProxiedPlayer)){
            cs.sendMessage("Only players can do this");
            return;
        }

        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                String message = "";
                for (String arg : args) {
                    message = message + arg + " ";
                }

                message = plugin.preparePlayerChat(message, (ProxiedPlayer) cs);
                message = plugin.replaceRegex(message);
                message = plugin.applyTagLogic(message);

                // replace variables
                String text = plugin.config.getString("chatFormat").replace("%player%",
                        plugin.wrapVariable(((ProxiedPlayer) cs).getDisplayName()));
                text = text.replace("%message%", plugin.wrapVariable(message));
                text = plugin.bukkitBridge.replaceVariables(((ProxiedPlayer) cs), text, "");

                // broadcast message
                BaseComponent[] msg = ChatParser.parse(text);
                for(ProxiedPlayer target: plugin.getProxy().getPlayers()){
                    if(plugin.ignoredPlayers.get(target.getName()) != null && plugin.ignoredPlayers.get(target.getName()).contains(cs.getName()))continue;
                    if(target.getServer() == null || !plugin.excludedServers.contains(target.getServer().getInfo().getName()))target.sendMessage(msg);
                }
            }
        });
    }
}
