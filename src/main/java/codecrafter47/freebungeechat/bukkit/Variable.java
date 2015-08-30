package codecrafter47.freebungeechat.bukkit;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;

public abstract class Variable {
    private final String name;

    protected Variable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String getReplacement(Player player);
}
