package club.sk1er.mods.tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Mod(modid = TechnoSaplingCounter.MODID, version = TechnoSaplingCounter.VERSION)
public class TechnoSaplingCounter {
    public static final String MODID = "techno-sapling-counter";
    public static final String VERSION = "1.0";
    public static int offset = 0;
    short tick = 0;
    private List<StatBase> saplings = new ArrayList<>();

    private static String func_180204_a(Item p_180204_0_) {
        ResourceLocation resourcelocation = Item.itemRegistry.getNameForObject(p_180204_0_);
        return resourcelocation != null ? resourcelocation.toString().replace(':', '.') : null;
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new CommandTreeOffset());
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        for (Item item : Item.itemRegistry) {
            String s = func_180204_a(item);
            if (s != null && s.contains("sapling")) {
                saplings.add(new StatCrafting("stat.useItem." + s, "", null, null));
                System.out.println("registering sapling: " + s);
            }
        }
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
            if (netHandler != null) {
                tick++;
                if (tick >= 20 * 15 + 2) //15.1 seconds
                    netHandler.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS));
            }
        }
    }

    public int getSaplingCount() {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
        if (thePlayer != null) {
            StatFileWriter statFileWriter = thePlayer.getStatFileWriter();
            if (statFileWriter != null) {
                int i = 0;
                for (StatBase sapling : saplings) {
                    i += statFileWriter.readStat(sapling);
                }
                return i + offset;
            }
        }
        return offset;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        String text = EnumChatFormatting.AQUA + "Trees Planted: " + getSaplingCount();

        int y = 3;
        int xTail = 3;
        int padding = 1;
        if (Minecraft.getMinecraft().theWorld == null) {
            return;
        }

        FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;
        int stringWidth = fontRendererObj.getStringWidth(text);
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

        Gui.drawRect(scaledResolution.getScaledWidth() - stringWidth - xTail - padding, y, scaledResolution.getScaledWidth() + stringWidth - xTail, y + 10, new Color(0, 0, 0, 100).getRGB());

        fontRendererObj.drawStringWithShadow(text, scaledResolution.getScaledWidth() - stringWidth - xTail, y + 1, Color.WHITE.getRGB());
    }
}
