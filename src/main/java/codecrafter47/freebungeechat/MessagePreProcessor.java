package codecrafter47.freebungeechat;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public interface MessagePreProcessor {

    String apply(ProxiedPlayer player, String message);
}
