package adris.altoclef.commands;

import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.util.helpers.ConfigHelper;
import gay.solonovamax.altoclef.AltoClef;

public class ReloadSettingsCommand extends Command {
    public ReloadSettingsCommand() {
        super("reload_settings", "Reloads bot settings and butler whitelist/blacklist.");
    }

    @Override
    protected void call(ArgParser parser) {
        ConfigHelper.reloadAllConfigs();
        AltoClef.INSTANCE.log("Reload successful!");
        finish();
    }
}
