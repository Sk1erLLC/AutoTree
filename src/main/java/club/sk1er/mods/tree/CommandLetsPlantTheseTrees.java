package club.sk1er.mods.tree;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CommandLetsPlantTheseTrees extends CommandBase {
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public String getCommandName() {
        return "letsplantthesetrees";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/letsplantthesetrees";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        TechnoSaplingCounter.running = !TechnoSaplingCounter.running;
        sender.addChatMessage(new ChatComponentText("Now: " + (TechnoSaplingCounter.running ? "Running" : "Paused")));
    }
}
