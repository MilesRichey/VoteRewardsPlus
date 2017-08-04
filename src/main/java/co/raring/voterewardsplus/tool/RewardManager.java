package co.raring.voterewardsplus.tool;

import co.raring.voterewardsplus.Core;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

//TODO: Cleanup this class, a lil messy
public class RewardManager {
    private static Core core;
    private static Map<Permission, Reward> rewards = new HashMap<>();
    private static Map<UUID, Integer> rewardsDue = new HashMap<>();
    private static FileConfiguration data;
    private static File dataFile;

    public static void init(Core core) {
        dataFile = new File(core.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                if (!dataFile.createNewFile()) {
                    throw new IOException("File already exists!");
                }
            } catch (IOException e) {
                core.getLogger().log(Level.SEVERE, e.getMessage(), e);
                core.getLogger().log(Level.WARNING, "Unable to create data file, disabling plugin.");
                core.getServer().getPluginManager().disablePlugin(core);
            }
        }
        data = new YamlConfiguration();
        try {
            data.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            core.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        RewardManager.core = core;

        // Load values from config into memory(rewardsDue map)
        core.getLogger().fine("Loading values from data.yml into memory.");
        ConfigurationSection owed = data.getConfigurationSection("votes-owed");
        if (owed != null) { //if config is blank
            Map<String, Object> values = owed.getValues(false);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (!(entry.getValue() instanceof Integer)) {
                    continue;
                }
                int times = (Integer) entry.getValue();
                core.getLogger().finest("Putting " + entry.toString());
                rewardsDue.put(UUID.fromString(entry.getKey()), times);
            }
        }
        ConfigurationSection rewardsSection = core.getConfig().getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                ConfigurationSection reward = rewardsSection.getConfigurationSection(key);
                int priority;
                double money = 0;
                int exp = 0;
                Collection<String> commands = new ArrayList<>();
                if (reward.contains("priority")) {
                    priority = reward.getInt("priority");
                } else {
                    core.getLogger().warning("Skipping group '".concat(key).concat("' because it's missing priority."));
                    continue;
                }
                if (reward.contains("money")) {
                    money = reward.getDouble("money");
                }
                if (reward.contains("exp")) {
                    exp = reward.getInt("exp");
                }
                if (reward.contains("commands")) {
                    commands.addAll(reward.getStringList("commands"));
                }
                Reward finalReward = new Reward(priority, money, exp, commands);
                Permission perm = new Permission("vrp.".concat(key));
                if (key.equalsIgnoreCase("default")) {
                    perm.setDefault(PermissionDefault.TRUE);
                } else {
                    perm.setDefault(PermissionDefault.FALSE);
                }
                perm.setDescription("Permission for the reward group ".concat(key).concat(" for VoteRewardsPlus"));
                if (Bukkit.getServer().getPluginManager().getPermission(perm.getName()) != null) {
                    core.getLogger().warning("Unable to register permission, '".concat(perm.getName()).concat("', may cause some issues.."));
                    // probably shouldnt idk:
                    Bukkit.getServer().getPluginManager().removePermission(perm.getName());
                    Bukkit.getServer().getPluginManager().addPermission(perm);
                } else {
                    Bukkit.getServer().getPluginManager().addPermission(perm);
                }
                rewards.put(perm, finalReward);

            }
        } else {
            core.getLogger().warning("Unable to find rewards section in config.yml!");
            core.getLogger().warning("Disabling plugin");
            core.getServer().getPluginManager().disablePlugin(core);
            return;
        }
    }

    public static void deinit(Core core) {
        for (Map.Entry<Permission, Reward> reward : rewards.entrySet()) {
            Bukkit.getPluginManager().removePermission(reward.getKey());
        }
        save();
    }

    public static void save() {
        if (!rewardsDue.isEmpty()) {
            for (Map.Entry<UUID, Integer> entry : rewardsDue.entrySet()) {
                String uuid = entry.getKey().toString();
                int times = entry.getValue();
                data.set("owed.".concat(uuid), times);
            }
        }
    }

    /**
     * @param plr Player to reward
     */
    public static void reward(Player plr) {
        //TODO: Optimize this
        Reward defaultReward = null;
        Reward finalReward = null;
        for (Map.Entry<Permission, Reward> entry : rewards.entrySet()) {
            if (plr.hasPermission(entry.getKey())) {
                if (finalReward != null) {
                    if (finalReward.getPriority() < entry.getValue().getPriority()) {
                        continue;
                    }
                }
                finalReward = entry.getValue();
            }
            if (entry.getKey().getName().equalsIgnoreCase("vrp.default")) {
                defaultReward = entry.getValue();
            }
        }
        if (finalReward == null) {
            if (defaultReward == null) {
                core.getLogger().severe("No default group found, disabling plugin.");
                core.getServer().getPluginManager().disablePlugin(core);
                return;
            }
            finalReward = defaultReward;
        }
        core.getLogger().warning("Couldn't find any reward groups...");
        // Handle reward
        for (String command : finalReward.getCommands()) {
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            core.getServer().dispatchCommand(Bukkit.getConsoleSender(), processString(command, plr));
        }
        if (finalReward.getExp() > 0) {
            plr.giveExpLevels(finalReward.getExp());
        }
        if (finalReward.getMoney() > 0) {
            Core.getEcon().depositPlayer(plr, finalReward.getMoney());
        }
        //TODO: Add customizable messages
        plr.sendMessage(Core.getPrefix().concat(processString("You just voted!", plr)));
    }

    /**
     * Increase the amount of votes a player is owed by one
     *
     * @param uuid Player to increase votes
     */
    public static void increase(UUID uuid) {
        if (rewardsDue.containsKey(uuid)) {
            rewardsDue.put(uuid, rewardsDue.get(uuid) + 1);
        } else {
            rewardsDue.put(uuid, 1);
        }
    }

    /**
     * @param uuid Player to get the votes from
     * @return How many times a player has voted but hasn't been rewarded or 0 if they don't have votes pending
     */
    public static int get(UUID uuid) {
        return rewardsDue.getOrDefault(uuid, 0);
    }

    /**
     * Processes a string and replaces all variables such as {PLAYER} to the players name
     *
     * @param str String to process and replace variables
     * @return Processed string
     */
    private static String processString(String str, Player plr) {
        if (str.toLowerCase().contains("{player}")) {
            str = str.replace("{player}", plr.getName());
        }
        return str;
    }
}
