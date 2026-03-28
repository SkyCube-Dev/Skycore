package dev.jackwith.skyCoreV2.utils;

import dev.jackwith.skyCoreV2.SkyCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Lang {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static void sendCommandLang(CommandSender sender, String path, String... replacements) {
        String message = SkyCore.getInstance().getLang().getString("pets." + path, path);

        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }

        boolean usePrefix = SkyCore.getInstance().getLang().getBoolean("pets.usePrefix", true);
        String prefix = SkyCore.getInstance().getLang().getString("pets.prefix", "[SKYPETS] ");

        Component component = mm.deserialize((usePrefix ? prefix : "") + message);

        if (sender instanceof Player player) {
            player.sendMessage(component);
        } else {
            sender.sendMessage(component.toString());
        }
    }

    public static Component getColored(String Text) { return mm.deserialize(Text); }
}
