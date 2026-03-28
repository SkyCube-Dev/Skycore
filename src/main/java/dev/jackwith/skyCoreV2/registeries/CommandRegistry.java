package dev.jackwith.skyCoreV2.registeries;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import dev.jackwith.skyCoreV2.SkyCore;

public class CommandRegistry {
    private final PaperCommandManager manager;

    public CommandRegistry(SkyCore plugin) {
        this.manager = new PaperCommandManager(plugin);
        this.manager.enableUnstableAPI("help");
    }

    public void registerAll(BaseCommand... commands) {
        for (BaseCommand command : commands) {
            manager.registerCommand(command);
        }
    }

    public PaperCommandManager getManager() {
        return manager;
    }
}