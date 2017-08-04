package co.raring.voterewardsplus.listener;

import co.raring.voterewardsplus.Core;
import co.raring.voterewardsplus.tool.RewardManager;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class RewardListener implements Listener {
    private Core core;

    public RewardListener(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onVote(VotifierEvent e) {
        Vote vote = e.getVote();
        if (core.getConfig().getBoolean("broadcast-votes")) {
            for (Player plr : Bukkit.getOnlinePlayers()) {
                if (plr.getName().equalsIgnoreCase(vote.getUsername())) {
                    continue;
                }
                plr.sendMessage(Core.getPrefix().concat(vote.getUsername().concat(" just voted!")));
            }
        }
        //TODO: Find better way to parse a username
        // Assume player is offline
        OfflinePlayer oplr = core.getServer().getOfflinePlayer(vote.getUsername());
        if (!oplr.isOnline()) {
            RewardManager.increase(oplr.getUniqueId());
        } else {
            Player plr = (Player) oplr;
            // Might be annoying
            if (core.getConfig().getBoolean("debug")) {
                if (plr.isOp() || plr.hasPermission("vrp.vote_info")) {
                    plr.sendMessage(Core.getPrefix().concat("{'username':'").concat(vote.getUsername()).concat("','address':'").concat(vote.getAddress()).concat("','service_name':'").concat(vote.getServiceName()).concat("','time_stamp':'").concat(vote.getTimeStamp()).concat("'"));
                }
            }
            //
            RewardManager.reward(plr);
        }

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player plr = e.getPlayer();
        int votes = RewardManager.get(plr.getUniqueId());
        if (votes != 0) {
            for (int i = 0; i < votes; i++) {
                RewardManager.reward(plr);
            }
        }
    }
}
