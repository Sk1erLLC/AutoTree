package club.sk1er.mods.tree;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class CommandTps extends CommandBase {
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public String getCommandName() {
        return "tps";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tps";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1) {
            Minecraft.getMinecraft().timer.ticksPerSecond = Integer.parseInt(args[0]);
        }
    }
}
