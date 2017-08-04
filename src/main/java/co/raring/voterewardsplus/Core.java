package co.raring.voterewardsplus;

import co.raring.voterewardsplus.cmd.CommandVRP;
import co.raring.voterewardsplus.listener.RewardListener;
import co.raring.voterewardsplus.tool.RewardManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;


public class Core extends JavaPlugin {
    private static Economy econ;
    private static String prefix = Colorize("&b[&cVoteRewards&6Plus&b]&7 ");

    /**
     * Replaces all occurences of & to a section sign(§) allowing Minecraft to parse the colors
     *
     * @param target Target string to colorize
     * @return Colorized string
     */
    public static String Colorize(String target) {
        return ChatColor.translateAlternateColorCodes('&', target);
    }

    /**
     * @return Prefix to append before all messages send out with this plugin
     */
    public static String getPrefix() {
        return prefix;
    }

    /**
     * @return Vault Economy
     */
    public static Economy getEcon() {
        return econ;
    }

    @Override
    public void onEnable() {
        // Check for Votifier
        Plugin assumedVotifier = getServer().getPluginManager().getPlugin("Votifier");
        if (assumedVotifier == null) {
            getLogger().warning("Unable to find Votifier! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Check for Vault
        Plugin assumedVault = getServer().getPluginManager().getPlugin("Vault");
        if (assumedVault == null) {
            getLogger().warning("Unable to find Vault! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Register Vault Economy
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Unable to setup Vault Economy! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        econ = rsp.getProvider();


        // Other stuff
        saveDefaultConfig();
        if (!new File(getDataFolder(), "data.yml").exists()) {
            saveResource("data.yml", false);
        }
        if (getConfig().getBoolean("debug")) { //Enable debug
            getLogger().setLevel(Level.FINEST);
            getLogger().finest("Enabled debug.");
        }
        prefix = Colorize(getConfig().getString("prefix")).concat(" ");

        RewardManager.init(this);
        PluginCommand cmd = getCommand("voterewardsplus");
        cmd.setUsage(prefix + Colorize("&cSyntax error! For usage run: /<command>"));
        cmd.setPermissionMessage(prefix + Colorize("&cYou do not have proper permissions to use this command."));
        cmd.setExecutor(new CommandVRP(this));
        getServer().getPluginManager().registerEvents(new RewardListener(this), this);
    }

    @Override
    public void onDisable() {
        RewardManager.deinit(this);
    }
}
