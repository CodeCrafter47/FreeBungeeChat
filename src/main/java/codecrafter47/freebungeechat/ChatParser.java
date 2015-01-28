package codecrafter47.freebungeechat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by florian on 25.01.15.
 */
public class ChatParser {
    private static Pattern pattern = Pattern.compile("(?ims)(?=\\n)|(?:[&\u00A7](?<color>[0-9A-FK-OR]))|" +
            "(?:\\[(?<tag>/?(?:b|i|u|s|nocolor|nobbcode)|(?:url|command|hover|suggest|color)=(?<value>(?:(?:[^]\\[]*)\\[(?:[^]\\[]*)\\])*(?:[^]\\[]*))|/(?:url|command|hover|suggest|color))\\])|" +
            "(?:\\[(?<implicitTag>url|command|suggest)\\](?=(?<implicitValue>.*?)\\[/\\k<implicitTag>\\]))");

    private static Pattern strip_bbcode_pattern = Pattern.compile("(?ims)(?:\\[(?<tag>/?(?:b|i|u|s|nocolor|nobbcode)|(?:url|command|hover|suggest|color)=(?<value>(?:(?:[^]\\[]*)\\[(?:[^]\\[]*)\\])*(?:[^]\\[]*))|/(?:url|command|hover|suggest|color))\\])|" +
            "(?:\\[(?<implicitTag>url|command|suggest)\\](?=(?<implicitValue>.*?)\\[/\\k<implicitTag>\\]))");

    public static BaseComponent[] parse(String text){
        Matcher matcher = pattern.matcher(text);
        TextComponent current = new TextComponent();
        List<BaseComponent> components = new LinkedList<>();
        int forceBold = 0;
        int forceItalic = 0;
        int forceUnderlined = 0;
        int forceStrikethrough = 0;
        int nocolorLevel = 0;
        int nobbcodeLevel = 0;
        Deque<ChatColor> colorDeque = new LinkedList<>();
        Deque<ClickEvent> clickEventDeque = new LinkedList<>();
        Deque<HoverEvent> hoverEventDeque = new LinkedList<>();
        while (matcher.find()){
            boolean parsed = false;
            {
                StringBuffer stringBuffer = new StringBuffer();
                matcher.appendReplacement(stringBuffer, "");
                TextComponent component = new TextComponent(current);
                current.setText(stringBuffer.toString());
                components.add(current);
                current = component;
            }
            String group_color = matcher.group("color");
            String group_tag = matcher.group("tag");
            String group_value = matcher.group("value");
            String group_implicitTag = matcher.group("implicitTag");
            String group_implicitValue = matcher.group("implicitValue");
            if(group_color != null && nocolorLevel <= 0){
                ChatColor color = ChatColor.getByChar(group_color.charAt(0));
                if(color != null) {
                    switch (color) {
                        case MAGIC:
                            current.setObfuscated(true);
                            break;
                        case BOLD:
                            current.setBold(true);
                            break;
                        case STRIKETHROUGH:
                            current.setStrikethrough(true);
                            break;
                        case UNDERLINE:
                            current.setUnderlined(true);
                            break;
                        case ITALIC:
                            current.setItalic(true);
                            break;
                        case RESET:
                            color = ChatColor.WHITE;
                        default:
                            current = new TextComponent();
                            current.setColor(color);
                            current.setBold(forceBold > 0);
                            current.setItalic(forceItalic > 0);
                            current.setUnderlined(forceUnderlined > 0);
                            current.setStrikethrough(forceStrikethrough > 0);
                            if(!colorDeque.isEmpty()){
                                current.setColor(colorDeque.peek());
                            }
                            if(!clickEventDeque.isEmpty()){
                                current.setClickEvent(clickEventDeque.peek());
                            }
                            if(!hoverEventDeque.isEmpty()){
                                current.setHoverEvent(hoverEventDeque.peek());
                            }
                    }
                    parsed = true;
                }
            }
            if(group_tag != null && nobbcodeLevel <= 0){
                // [b]this is bold[/b]
                if (group_tag.matches("(?i)^b$")){
                    forceBold++;
                    if (forceBold > 0){
                        current.setBold(true);
                    } else {
                        current.setBold(false);
                    }
                    parsed = true;
                } else if(group_tag.matches("(?i)^/b$")){
                    forceBold--;
                    if (forceBold <= 0){
                        current.setBold(false);
                    } else{
                        current.setBold(true);
                    }
                    parsed = true;
                }
                // [i]this is italic[/i]
                if (group_tag.matches("(?i)^i$")){
                    forceItalic++;
                    if (forceItalic > 0){
                        current.setItalic(true);
                    } else {
                        current.setItalic(false);
                    }
                    parsed = true;
                } else if(group_tag.matches("(?i)^/i$")){
                    forceItalic--;
                    if (forceItalic <= 0){
                        current.setItalic(false);
                    } else{
                        current.setItalic(true);
                    }
                    parsed = true;
                }
                // [u]this is underlined[/u]
                if (group_tag.matches("(?i)^u$")){
                    forceUnderlined++;
                    if (forceUnderlined > 0){
                        current.setUnderlined(true);
                    } else {
                        current.setUnderlined(false);
                    }
                    parsed = true;
                } else if(group_tag.matches("(?i)^/u$")){
                    forceUnderlined--;
                    if (forceUnderlined <= 0){
                        current.setUnderlined(false);
                    } else{
                        current.setUnderlined(true);
                    }
                    parsed = true;
                }
                // [s]this is crossed out[/s]
                if (group_tag.matches("(?i)^s$")){
                    forceStrikethrough++;
                    if (forceStrikethrough > 0){
                        current.setStrikethrough(true);
                    } else {
                        current.setStrikethrough(false);
                    }
                    parsed = true;
                } else if(group_tag.matches("(?i)^/s$")){
                    forceStrikethrough--;
                    if (forceStrikethrough <= 0){
                        current.setStrikethrough(false);
                    } else{
                        current.setStrikethrough(true);
                    }
                    parsed = true;
                }
                // [color=red]huh this is red...[/color]
                if(group_tag.matches("(?i)^color=.*$")){
                    ChatColor color = null;
                    for(ChatColor color1: ChatColor.values()){
                        if (color1.getName().equalsIgnoreCase(group_value)){
                            color = color1;
                        }
                    }
                    colorDeque.push(current.getColor());
                    if(color != null && color != ChatColor.BOLD && color != ChatColor.ITALIC && color != ChatColor.MAGIC
                            && color != ChatColor.RESET && color != ChatColor.STRIKETHROUGH && color != ChatColor.UNDERLINE){
                        colorDeque.push(color);
                        current.setColor(color);
                    } else {
                        ProxyServer.getInstance().getLogger().warning("Invalid color tag: [" + group_tag + "] UNKNOWN COLOR '" + group_value + "'");
                        colorDeque.push(ChatColor.WHITE);
                        current.setColor(ChatColor.WHITE);
                    }
                    parsed = true;
                } else if(group_tag.matches("(?i)^/color$")){
                    if(!colorDeque.isEmpty()){
                        colorDeque.pop();
                        current.setColor(colorDeque.pop());
                    }
                    parsed = true;
                }
                // [url=....]
                if(group_tag.matches("(?i)^url=.*$")){
                    String url = group_value;
                    if(!url.startsWith("http")){
                        url = "http://" + url;
                    }
                    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
                    clickEventDeque.push(clickEvent);
                    current.setClickEvent(clickEvent);
                    parsed = true;
                }
                // [/url], [/command], [/suggest]
                if(group_tag.matches("(?i)^/(?:url|command|suggest)$")){
                    if(!clickEventDeque.isEmpty())clickEventDeque.pop();
                    if(clickEventDeque.isEmpty()){
                        current.setClickEvent(null);
                    } else {
                        current.setClickEvent(clickEventDeque.peek());
                    }
                    parsed = true;
                }
                // [command=....]
                if(group_tag.matches("(?i)^command=.*")){
                    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, group_value);
                    clickEventDeque.push(clickEvent);
                    current.setClickEvent(clickEvent);
                    parsed = true;
                }
                // [suggest=....]
                if(group_tag.matches("(?i)^suggest=.*")){
                    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, group_value);
                    clickEventDeque.push(clickEvent);
                    current.setClickEvent(clickEvent);
                    parsed = true;
                }
                // [hover=....]...[/hover]
                if(group_tag.matches("(?i)^hover=.*$")){
                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, parse(group_value));
                    hoverEventDeque.push(hoverEvent);
                    current.setHoverEvent(hoverEvent);
                    parsed = true;
                } else if(group_tag.matches("(?i)^/hover$")){
                    if(!hoverEventDeque.isEmpty())hoverEventDeque.pop();
                    if(hoverEventDeque.isEmpty()){
                        current.setHoverEvent(null);
                    } else {
                        current.setHoverEvent(hoverEventDeque.peek());
                    }
                    parsed = true;
                }
            }
            if(group_implicitTag != null && nobbcodeLevel <= 0){
                // [url]spigotmc.org[/url]
                if(group_implicitTag.matches("(?i)^url$")){
                    String url = group_implicitValue;
                    if(!url.startsWith("http")){
                        url = "http://" + url;
                    }
                    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
                    clickEventDeque.push(clickEvent);
                    current.setClickEvent(clickEvent);
                    parsed = true;
                }
                // [command]/spawn[/command]
                if(group_implicitTag.matches("(?i)^command$")){
                    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, group_implicitValue);
                    clickEventDeque.push(clickEvent);
                    current.setClickEvent(clickEvent);
                    parsed = true;
                }
                // [suggest]/friend add [/suggest]
                if(group_implicitTag.matches("(?i)^suggest$")){
                    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, group_implicitValue);
                    clickEventDeque.push(clickEvent);
                    current.setClickEvent(clickEvent);
                    parsed = true;
                }
            }
            if(group_tag != null){
                if(group_tag.matches("(?i)^nocolor$")){
                    nocolorLevel++;
                    parsed = true;
                }
                if(group_tag.matches("(?i)^/nocolor$")){
                    nocolorLevel--;
                    parsed = true;
                }
                if(group_tag.matches("(?i)^nobbcode$")){
                    nobbcodeLevel++;
                    parsed = true;
                }
                if(group_tag.matches("(?i)^/nobbcode$")){
                    nobbcodeLevel--;
                    parsed = true;
                }
            }
            if(!parsed){
                TextComponent component = new TextComponent(current);
                current.setText(matcher.group(0));
                components.add(current);
                current = component;
            }
        }
        StringBuffer stringBuffer = new StringBuffer();
        matcher.appendTail(stringBuffer);
        current.setText(stringBuffer.toString());
        components.add(current);
        return components.toArray(new BaseComponent[components.size()]);
    }

    public static String stripBBCode(String string){
        return strip_bbcode_pattern.matcher(string).replaceAll("");
    }
}
