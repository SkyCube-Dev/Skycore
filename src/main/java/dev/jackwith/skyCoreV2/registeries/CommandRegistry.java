package dev.jackwith.skyCoreV2.registeries;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import dev.jackwith.skyCoreV2.SkyCore;

public record CommandRegistry(PaperCommandManager manager) {
    public CommandRegistry(SkyCore manager) {
        this(new PaperCommandManager(manager));
        this.manager.enableUnstableAPI("help");
    }

    public void registerAll(BaseCommand... commands) {
        for (BaseCommand command : commands) {
            manager.registerCommand(command);
        }
    }
}