package codecrafter47.freebungeechat.extensions;

import codecrafter47.freebungeechat.FreeBungeeChat;
import codecrafter47.freebungeechat.MessagePreProcessor;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ColoredChat extends Extension {
    private transient FreeBungeeChat plugin = null;
    private transient Map<String, String> colorMap;
    private transient Map<String, String> effectMap;

    private boolean saveColors = true;
    private List<String> colorCommandAliases = Lists.newArrayList("setcolor", "chatcolor");
    private List<String> effectCommandAliases = Lists.newArrayList("seteffect", "chateffect");
    private String msgNotColored = "You chat is not colored anymore.";
    private String msgNoEffect = "Your chat is clear from any effects.";

    private static Set<ChatColor> colors = Sets.newLinkedHashSet(Arrays.asList(
            ChatColor.BLACK,
            ChatColor.DARK_BLUE,
            ChatColor.DARK_GREEN,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_RED,
            ChatColor.DARK_PURPLE,
            ChatColor.GOLD,
            ChatColor.GRAY,
            ChatColor.DARK_GRAY,
            ChatColor.BLUE,
            ChatColor.GREEN,
            ChatColor.AQUA,
            ChatColor.RED,
            ChatColor.LIGHT_PURPLE,
            ChatColor.YELLOW,
            ChatColor.WHITE,
            ChatColor.RESET
    ));
    private static Set<ChatColor> effects = Sets.newLinkedHashSet(Arrays.asList(
            ChatColor.MAGIC,
            ChatColor.BOLD,
            ChatColor.STRIKETHROUGH,
            ChatColor.UNDERLINE,
            ChatColor.ITALIC,
            ChatColor.RESET
    ));

    public boolean isSaveColors() {
        return saveColors;
    }

    public void setSaveColors(boolean saveColors) {
        this.saveColors = saveColors;
    }

    public List<String> getColorCommandAliases() {
        return colorCommandAliases;
    }

    public void setColorCommandAliases(List<String> colorCommandAliases) {
        this.colorCommandAliases = colorCommandAliases;
    }

    public List<String> getEffectCommandAliases() {
        return effectCommandAliases;
    }

    public void setEffectCommandAliases(List<String> effectCommandAliases) {
        this.effectCommandAliases = effectCommandAliases;
    }

    public String getMsgNotColored() {
        return msgNotColored;
    }

    public void setMsgNotColored(String msgNotColored) {
        this.msgNotColored = msgNotColored;
    }

    public String getMsgNoEffect() {
        return msgNoEffect;
    }

    public void setMsgNoEffect(String msgNoEffect) {
        this.msgNoEffect = msgNoEffect;
    }

    @Override
    public void onEnable(FreeBungeeChat plugin) {
        this.plugin = plugin;
        if (saveColors) {
            loadSavedData();
        }
        if (!colorCommandAliases.isEmpty()) {
            plugin.getProxy().getPluginManager().registerCommand(plugin, new SetColorCommand(colorCommandAliases.get(0), "freebungeechat.command.chatcolor",
                    colorCommandAliases.subList(1, colorCommandAliases.size()).toArray(new String[colorCommandAliases.size() - 1])));
        }
        if (!effectCommandAliases.isEmpty()) {
            plugin.getProxy().getPluginManager().registerCommand(plugin, new SetEffectCommand(effectCommandAliases.get(0), "freebungeechat.command.chateffect",
                    effectCommandAliases.subList(1, effectCommandAliases.size()).toArray(new String[effectCommandAliases.size() - 1])));
        }
        plugin.registerMessagePreprocessor(new MessagePreProcessor() {
            @Override
            public String apply(ProxiedPlayer player, String message) {
                return getPrefix(player) + message;
            }
        });
    }

    private String getPrefix(ProxiedPlayer player) {
        String prefix = "";
        String color = colorMap.get(player.getUniqueId().toString());
        if(color != null){
            prefix += color;
        }
        String effect = effectMap.get(player.getUniqueId().toString());
        if(effect != null){
            prefix += effect;
        }
        return prefix;
    }

    @Override
    public void onDisable() {
        saveData();
    }

    @SuppressWarnings("unchecked")
    private void loadSavedData() {
        File saveFile = new File(plugin.getDataFolder(), "chatColors.yml");
        colorMap = new ConcurrentHashMap<>();
        effectMap = new ConcurrentHashMap<>();
        if (saveFile.exists()) {
            try {
                Yaml yaml = new Yaml();
                Map<String, Map<String, String>> data = (Map<String, Map<String, String>>) yaml.load(new InputStreamReader(new FileInputStream(saveFile), Charsets.UTF_8));
                colorMap.putAll(data.get("colorMap"));
                effectMap.putAll(data.get("effectMap"));
            } catch (Throwable th) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load chatColor data. Saved Data is lost.", th);
            }
        }
    }

    private void saveData() {
        File saveFile = new File(plugin.getDataFolder(), "chatColors.yml");
        Map<String, Map<String, String>> data = new HashMap<>();
        data.put("colorMap", colorMap);
        data.put("effectMap", effectMap);
        try {
            if (saveFile.exists()) {
                saveFile.delete();
            }
            saveFile.createNewFile();
            Yaml yaml = new Yaml();
            OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(saveFile, true), Charsets.UTF_8);
            yaml.dump(data, output);
            output.close();
        } catch (Throwable th) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save chatColor data.", th);
        }
    }

    private class SetColorCommand extends Command {
        public SetColorCommand(String name, String permission, String... aliases) {
            super(name, permission, aliases);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(new ComponentBuilder("This command has been made for players only.").color(ChatColor.RED).create());
                return;
            }
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (args.length == 0) {
                player.sendMessage(Joiner.on(' ').join(Iterables.transform(colors, new Function<ChatColor, String>() {
                    @Override
                    public String apply(ChatColor chatColor) {
                        return "" + chatColor + chatColor.getName() + ChatColor.RESET;
                    }
                })));
                return;
            }
            ChatColor chatColor = null;
            if (args[0].length() == 1) {
                chatColor = ChatColor.getByChar(args[0].toLowerCase().charAt(0));
            } else if (args[0].length() == 2) {
                chatColor = ChatColor.getByChar(args[0].toLowerCase().charAt(1));
            } else {
                try {
                    chatColor = ChatColor.valueOf(args[0].toUpperCase());
                } catch (Throwable ignored) {
                }
            }
            if(chatColor == null){
                player.sendMessage(new ComponentBuilder(String.format("Unknown color '%s'", args[0])).color(ChatColor.RED).create());
                return;
            }
            if(!colors.contains(chatColor)){
                player.sendMessage(new ComponentBuilder(String.format("'%s' is not a color. Use /seteffect instead.", args[0])).color(ChatColor.RED).create());
                return;
            }
            String permission = String.format("freebungeechat.chatcolor.color.%s", chatColor.getName().toLowerCase());
            if(chatColor != ChatColor.RESET && !player.hasPermission(permission)){
                player.sendMessage(new ComponentBuilder("To use this color you require the permission ").color(ChatColor.RED).append(permission).append(" which you don't have.").create());
                return;
            }
            if(chatColor == ChatColor.RESET){
                colorMap.put(player.getUniqueId().toString(), "");
                player.sendMessage(plugin.getChatParser().parse(msgNotColored));
            } else {
                colorMap.put(player.getUniqueId().toString(), chatColor.toString());
                player.sendMessage(new ComponentBuilder("Your chat is now colored in ").append(chatColor.getName()).color(chatColor).append("!", ComponentBuilder.FormatRetention.NONE).create());
            }
        }
    }

    private class SetEffectCommand extends Command {
        public SetEffectCommand(String name, String permission, String... aliases) {
            super(name, permission, aliases);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(new ComponentBuilder("This command has been made for players only.").color(ChatColor.RED).create());
                return;
            }
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (args.length == 0) {
                player.sendMessage(Joiner.on(' ').join(Iterables.transform(effects, new Function<ChatColor, String>() {
                    @Override
                    public String apply(ChatColor chatColor) {
                        return "" + chatColor + chatColor.getName() + ChatColor.RESET;
                    }
                })));
                return;
            }
            ChatColor chatColor = null;
            if (args[0].length() == 1) {
                chatColor = ChatColor.getByChar(args[0].toLowerCase().charAt(0));
            } else if (args[0].length() == 2) {
                chatColor = ChatColor.getByChar(args[0].toLowerCase().charAt(1));
            } else {
                try {
                    chatColor = ChatColor.valueOf(args[0].toUpperCase());
                } catch (Throwable ignored) {
                }
            }
            if(chatColor == null){
                player.sendMessage(new ComponentBuilder(String.format("Unknown color '%s'", args[0])).color(ChatColor.RED).create());
                return;
            }
            if(!effects.contains(chatColor)){
                player.sendMessage(new ComponentBuilder(String.format("'%s' is not an effect. Use /setcolor instead.", args[0])).color(ChatColor.RED).create());
                return;
            }
            String permission = String.format("freebungeechat.chatcolor.effect.%s", chatColor.getName().toLowerCase());
            if(chatColor != ChatColor.RESET && !player.hasPermission(permission)){
                player.sendMessage(new ComponentBuilder("To use this effect you require the permission ").color(ChatColor.RED).append(permission).append(" which you don't have.").create());
                return;
            }
            if(chatColor == ChatColor.RESET){
                effectMap.put(player.getUniqueId().toString(), "");
                player.sendMessage(plugin.getChatParser().parse(msgNoEffect));
            } else {
                effectMap.put(player.getUniqueId().toString(), chatColor.toString());
                player.sendMessage(new ComponentBuilder("Your chat is now ").append(chatColor.getName()).color(chatColor).append("!", ComponentBuilder.FormatRetention.NONE).create());
            }
        }
    }
}
