package club.sk1er.mods.tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Mod(modid = TechnoSaplingCounter.MODID, version = TechnoSaplingCounter.VERSION)
public class TechnoSaplingCounter {

    public static final boolean ENABLE_DANGEROUS_STUFF = true;


    public static final String MODID = "techno-sapling-counter";
    public static final String VERSION = "1.0";
    /*
    #Index
    0 -> idle
    1 -> move to loc & look at loc

//    2 -> place tree
    #not used yet
//    3 -> bone meal 1
//    4 -> bone meal 2
//    5 -> bone meal 3

     */
    public static final int TREE_GOAL = 1_000_000;

    public static int offset = 0;
    public static boolean running = false;
    public static byte index = 0;
    public static int currentTree = 0;
    public static int currentTick = 0;
    public static double relT = 0;
    public int ticksPerAction = 1;
    private short tick = 0;
    private List<StatBase> saplings = new ArrayList<>();

    private static String func_180204_a(Item p_180204_0_) {
        ResourceLocation resourcelocation = Item.itemRegistry.getNameForObject(p_180204_0_);
        return resourcelocation != null ? resourcelocation.toString().replace(':', '.') : null;
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new CommandTreeOffset());
        if (ENABLE_DANGEROUS_STUFF) {
            ClientCommandHandler.instance.registerCommand(new CommandLetsPlantTheseTrees());
            ClientCommandHandler.instance.registerCommand(new CommandTreePlantOffset());
        }
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
            if (running) {
                currentTick++;
                if (currentTick % ticksPerAction == 0) {
                    tickAI();
                }
            } else {
                NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
                if (netHandler != null) {
                    tick++;
                    if (tick >= 20 * 15 + 2) {
                        netHandler.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS));
                        tick = 0;
                    }
                }
            }
        }
    }

    public int getSaplingCount() {
        if (running || ENABLE_DANGEROUS_STUFF) return currentTree;

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

    private void tickAI() {
        if (!ENABLE_DANGEROUS_STUFF) return;
        if (!running) return;
        if (currentTree == TREE_GOAL) return;
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
        if (thePlayer == null) return;

        index++;
        if (index == 3) {
            index = 1;
            currentTree++;
        }


        Pos playerPos = getPos(currentTick);
//        switch (index) {
//            case 1: {
//                double offset = 4;
//                BlockPos playerPos = posForCurrentTree.south((int) offset);

        System.out.println(Math.pow(thePlayer.posX - thePlayer.lastTickPosX, 2) + Math.pow(thePlayer.posZ - thePlayer.lastTickPosZ, 2));
        float atan = (float) Math.atan(1D / offset);
        for (WorldServer worldServer : Minecraft.getMinecraft().getIntegratedServer().worldServers) {
            for (EntityPlayer playerEntity : worldServer.playerEntities) {
                if (playerEntity.getUniqueID() == Minecraft.getMinecraft().thePlayer.getUniqueID()) {
                    ((EntityPlayerMP) playerEntity).playerNetServerHandler.setPlayerLocation(playerPos.x, 4, playerPos.z, 180, 0); //Tan -1 .5
                }
            }
//                }
//                break;
//            }
//            case 2: {
//                Minecraft.getMinecraft().playerController.onPlayerRightClick(thePlayer, Minecraft.getMinecraft().theWorld, thePlayer.getHeldItem(), posForCurrentTree, EnumFacing.UP, new Vec3(0, 0, 0));
//                break;
//            }
        }

    }

    private Pos getPos(int tick) {

        double v = 3D / 2D * Math.PI;
        relT += .1D / (Math.PI * 2 * Math.toRadians(tick / v));
        double x = relT * Math.cos(relT) / v;
        double z = relT * Math.sin(relT) / v;
        return new Pos(x, z);
    }

    private BlockPos getPosForCurrentTree(int y) {
        int column = currentTree / 1000;
        int row = currentTree % 1000;
        if (column % 2 == 0)
            row = 1000 - row;
        return new BlockPos(row, y, column);
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
        render(text, y, xTail, padding, fontRendererObj, stringWidth, scaledResolution);
        if (ENABLE_DANGEROUS_STUFF) {
            y += 10;
            text = "Progress: " + (currentTree) + "/" + TREE_GOAL;
            stringWidth = fontRendererObj.getStringWidth(text);
            render(text, y, xTail, padding, fontRendererObj, stringWidth, scaledResolution);

            y += 10;
            text = Math.round(currentTree * 100000D / ((double) TREE_GOAL)) / 1000D + "%";
            render(text, y, xTail, padding, fontRendererObj, fontRendererObj.getStringWidth(text), scaledResolution);


            y += 10;
            long ms = currentTree * 100;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'Hours:' H 'Minutes:' m 'Seconds:' s");
            boolean flag = ms > TimeUnit.DAYS.toMillis(1);
            if (flag) {
                ms -= TimeUnit.DAYS.toMillis(1);
                simpleDateFormat = new SimpleDateFormat("'Day:' d 'Hours:' H 'Minutes:' m 'Seconds:' s");
            }

            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            text = (flag ? "Day: 0 " : "") + simpleDateFormat.format(new Date(ms));

            render(text, y, xTail, padding, fontRendererObj, fontRendererObj.getStringWidth(text), scaledResolution);


        }
    }

    private void render(String text, int y, int xTail, int padding, FontRenderer fontRendererObj, int stringWidth, ScaledResolution scaledResolution) {
        Gui.drawRect(scaledResolution.getScaledWidth() - stringWidth - xTail - padding, y, scaledResolution.getScaledWidth() + stringWidth - xTail, y + 10, new Color(0, 0, 0, 100).getRGB());
        fontRendererObj.drawStringWithShadow(text, scaledResolution.getScaledWidth() - stringWidth - xTail, y + 1, Color.WHITE.getRGB());
    }

    class Pos {
        double x, z;

        public Pos(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }
}
