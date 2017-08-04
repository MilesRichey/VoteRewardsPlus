package co.raring.voterewardsplus.cmd;

import co.raring.voterewardsplus.Core;
import co.raring.voterewardsplus.listener.RewardListener;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CommandVRP implements CommandExecutor {
    private static final String[] HELP = new String[]{
            "/vrp reload - reloads config.yml",
            "/vrp test <player> - simulates a player vote"
    };

    static {
        for (int i = 0; i < HELP.length; i++) {
            HELP[i] = Core.Colorize(HELP[i]);
        }
    }

    private Core core;

    public CommandVRP(Core core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player || sender instanceof ConsoleCommandSender) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    core.reloadConfig();
                    sender.sendMessage(Core.getPrefix().concat("Reloaded config!"));
                } else {
                    return args[0].equalsIgnoreCase("help") && sendHelp(sender);
                }
                return true;
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("test")) {
                    Player plr = Bukkit.getPlayer(args[1]);
                    if (plr == null) {
                        sender.sendMessage(Core.getPrefix() + ChatColor.RED + "Invalid player!");
                        return true;
                    }
                    Vote vote = new Vote();
                    vote.setAddress(plr.getAddress().getHostName());
                    vote.setServiceName("Test-Vote");
                    vote.setTimeStamp(String.valueOf(Math.toIntExact(System.currentTimeMillis() / 1000)));
                    vote.setUsername(plr.getName());
                    VotifierEvent ve = new VotifierEvent(vote);
                    new RewardListener(core).onVote(ve);
                } else {
                    return false;
                }
                return true;
            } else {
                return sendHelp(sender);
            }
        } else {
            sender.sendMessage("Only console & players can use this command.");
            return true;
        }
    }

    private boolean sendHelp(CommandSender sender) {
        sender.sendMessage(HELP);
        return true;
    }
}
