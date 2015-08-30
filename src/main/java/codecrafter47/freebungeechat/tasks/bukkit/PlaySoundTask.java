package codecrafter47.freebungeechat.tasks.bukkit;

import codecrafter47.freebungeechat.bukkit.FreeBungeeChatBukkit;
import codecrafter47.freebungeechat.tasks.BukkitTask;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PlaySoundTask extends BukkitTask {
    private final String soundId;

    public PlaySoundTask(String soundId) {
        this.soundId = soundId;
    }

    @Override
    public void execute(FreeBungeeChatBukkit plugin, Player player) {
        try {
            Sound sound = Sound.valueOf(this.soundId);
            player.playSound(player.getLocation(), sound, 5, 1);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Sound with id \"" + soundId + "\" does not exist");
        }
    }
}
