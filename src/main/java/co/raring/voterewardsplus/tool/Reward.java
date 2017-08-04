package co.raring.voterewardsplus.tool;

import java.util.Collection;

public class Reward {
    private final int priority;
    private final double money;
    private final int expLevels;
    private final Collection<String> commands;

    /**
     * @param priority  Priority of group
     * @param money     How much money to reward upon voting
     * @param expLevels How many exp levels to donate upon voting
     * @param commands  What commands to execute upon voting
     */
    public Reward(int priority, double money, int expLevels, Collection<String> commands) {
        this.priority = priority;
        this.money = money;
        this.expLevels = expLevels;
        if (commands != null) {
            this.commands = commands;
        } else {
            this.commands = null;
        }
    }

    /**
     * @return Priority of this group
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @return Amount of money this reward gives
     */
    public double getMoney() {
        return money;
    }

    /**
     * @return Amount of exp in levels will be rewarded
     */
    public int getExp() {
        return expLevels;
    }

    /**
     * @return Commands to execute upon voting
     */
    public Collection<String> getCommands() {
        return commands;
    }
}
