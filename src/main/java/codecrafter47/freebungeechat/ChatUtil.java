/*
 * Copyright (C) 2014 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codecrafter47.freebungeechat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Copyright (C) 2014 florian
 */
public class ChatUtil {

    static Pattern pattern = Pattern.compile(
            "(?:(?<!\\\\)(?:(?=(?:[*_]{2,2}) )|(?<= ))(?<strong>[*_]{2,2}))|"
                    + "(?:(?<!\\\\)(?:(?=[*_] )|(?<= ))(?<em>[*_]))|"
                    + "(?:(?<!\\\\)[§&](?<color>[" + ChatColor.ALL_CODES + "]))|"
                    + "(?:(?<!\\\\)<(?<directlink>[^<>]+)>)|"
                    + "(?:(?<!\\\\)\\[(?<text>[^\\]]*?)(?<!\\\\)\\](?=[({])(?:\\((?<url>.*?)(?<!\\\\)\\))?(?:\\{(?<tooltip>.*?)(?<!\\\\)\\})?)|"
                    + "(?:(?<!\\\\)\\[(?<cmdtext>[^\\]]*?)(?<!\\\\)\\](?=[\\[{])(?:\\[(?<cmdurl>.*?)(?<!\\\\)\\])?(?:\\{(?<cmdtooltip>.*?)(?<!\\\\)\\})?)",
			Pattern.DOTALL
    );

    public static BaseComponent[] parseString(String s) {
        return iparseString(new ComponentBuilder(""), s).create();
    }

    public static ComponentBuilder iparseString(ComponentBuilder cb, String s) {

        Matcher matcher = pattern.matcher(s);
        boolean bold = false;
        boolean italic = false;

        while (matcher.find()) {
            StringBuffer sb = new StringBuffer();
            matcher.appendReplacement(sb, "");
            String str = sb.toString();
            str = removeEscapes(str);
            cb = cb.append(str);
            cb = cb.append("");

            if (matcher.group("strong") != null) {
                cb = cb.bold(bold = !bold);
            }
            if (matcher.group("em") != null) {
                cb = cb.italic(italic = !italic);
            }
            if (matcher.group("color") != null) {
                cb = cb.color(ChatColor.getByChar(matcher.group("color").charAt(
                        0)));
            }
            if (matcher.group("text") != null) {
                String url = matcher.group("url");
                String hover = matcher.group("tooltip");
                if (FreeBungeeChat.instance.config.getBoolean("underlineLinks", true))
                    cb = cb.underlined(true);
                if (url != null) {
                    ClickEvent evt;
                    if (url.charAt(0) == '/') {
                        evt = new ClickEvent(ClickEvent.Action.RUN_COMMAND, url);
                    } else {
                        evt = new ClickEvent(ClickEvent.Action.OPEN_URL,
                                makeLink(url));
                    }
                    cb = cb.event(evt);
                }
                if (hover != null) {
                    cb = cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            parseString(hover)));
                }
                cb = iparseString(cb, matcher.group("text"));
                cb = cb.append("");
                cb = cb.event((ClickEvent) null);
                cb = cb.event((HoverEvent) null);
                if (FreeBungeeChat.instance.config.getBoolean("underlineLinks", true))
                    cb = cb.underlined(false);
            }
            if (matcher.group("cmdtext") != null) {
                String url = matcher.group("cmdurl");
                String hover = matcher.group("cmdtooltip");
                if (FreeBungeeChat.instance.config.getBoolean("underlineLinks", true))
                    cb = cb.underlined(true);
                if (url != null) {
                    ClickEvent evt;
                    if (url.charAt(0) == '/') {
                        evt = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                url);
                    } else {
                        evt = new ClickEvent(ClickEvent.Action.OPEN_URL,
                                makeLink(url));
                    }
                    cb = cb.event(evt);
                }
                if (hover != null) {
                    cb = cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            parseString(hover)));
                }
                cb = iparseString(cb, matcher.group("cmdtext"));
                cb = cb.append("");
                cb = cb.event((ClickEvent) null);
                cb = cb.event((HoverEvent) null);
                if (FreeBungeeChat.instance.config.getBoolean("underlineLinks", true))
                    cb = cb.underlined(false);
            }
            if (matcher.group("directlink") != null) {
                if (FreeBungeeChat.instance.config.getBoolean("underlineLinks", true))
                    cb = cb.underlined(true);
                cb = cb.event(new ClickEvent(ClickEvent.Action.OPEN_URL,
                        makeLink(matcher.group("directlink"))));
                cb = cb.append(matcher.group("directlink"));
                cb = cb.append("");
                cb = cb.event((ClickEvent) null);
                cb = cb.event((HoverEvent) null);
                if (FreeBungeeChat.instance.config.getBoolean("underlineLinks", true))
                    cb = cb.underlined(false);

            }
        }

        StringBuffer sb = new StringBuffer();
        matcher.appendTail(sb);
        cb = cb.append(removeEscapes(sb.toString()));

        return cb;
    }

    private static String removeEscapes(String str) {
		return str.replaceAll("\\\\(?<rep>[\\[\\]()<>*{}§&\\\\])", "${rep}");
    }

	public static String escapeSpecialChars(String str) {
		return str.replaceAll("(?<rep>[\\[\\]()<>*{}§&\\\\])", "\\\\${rep}");
	}

    private static String makeLink(String link) {
        if (!link.matches("http((s)?)://.*")) {
            return "http://" + link;
        }
        return link;
    }
}
