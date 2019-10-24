package club.sk1er.mods.tree;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import org.apache.commons.lang3.math.NumberUtils;

public class CommandSetTreeGoal extends CommandBase {
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public String getCommandName() {
        return "settreegoal";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/settreegoal <num>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if(args.length != 1 || !NumberUtils.isNumber(args[0].replaceFirst("-",""))) {
            sender.addChatMessage(new ChatComponentText("/settreegoal <number>"));
            sender.addChatMessage(new ChatComponentText("current goal is " +  TechnoSaplingCounter.TREE_GOAL));

        } else {
            TechnoSaplingCounter.TREE_GOAL = Integer.parseInt(args[0]);
            sender.addChatMessage(new ChatComponentText("Set tree goal to " + TechnoSaplingCounter.TREE_GOAL));
        }
    }
}
