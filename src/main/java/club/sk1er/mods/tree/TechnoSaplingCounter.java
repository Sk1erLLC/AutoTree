package club.sk1er.mods.tree;

import ch.obermuhlner.math.big.BigDecimalMath;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * This was created for the #TeamTrees fundraiser. Code is not high quality and was written quickly.
 */
@Mod(modid = TechnoSaplingCounter.MODID, version = TechnoSaplingCounter.VERSION)
public class TechnoSaplingCounter {

    public static final boolean ENABLE_DANGEROUS_STUFF = true;


    public static final String MODID = "team-trees-mod";
    public static final String VERSION = "1.0";
    public static int TREE_GOAL = 20_000_000;
    public static int offset = 0;
    public static boolean running = false;
    public static byte index = 0;
    public static int currentTree = 0;
    public static int currentTick = 0;
    public int ticksPerAction = 1;
    private short tick = 0;
    private List<StatBase> saplings = new ArrayList<>();

    private static String func_180204_a(Item p_180204_0_) {
        ResourceLocation resourcelocation = Item.itemRegistry.getNameForObject(p_180204_0_);
        return resourcelocation != null ? resourcelocation.toString().replace(':', '.') : null;
    }

    private Pos getPos(BigDecimal tick) { //Use BigDecimal to maintain precision at high magnitudes

        MathContext mathContext = new MathContext(128);
        BigDecimal a = tick.multiply(new BigDecimal(3));
        BigDecimal theta = BigDecimalMath.sqrt(a, mathContext);

        BigDecimal x = theta.multiply(BigDecimalMath.cos(theta, mathContext));
        BigDecimal z = theta.multiply(BigDecimalMath.sin(theta, mathContext));
        return new Pos(x.doubleValue(), z.doubleValue());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new CommandSetCurrentTick());
        if (ENABLE_DANGEROUS_STUFF) {
            ClientCommandHandler.instance.registerCommand(new CommandLetsPlantTheseTrees());
            ClientCommandHandler.instance.registerCommand(new CommandTreePlantOffset());
            ClientCommandHandler.instance.registerCommand(new CommandTps());
            ClientCommandHandler.instance.registerCommand(new CommandSetTreeGoal());
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
        if (event.phase == TickEvent.Phase.START) { //Do at start of tick so it processes that tick
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

    private void updateState() {//Make server allow our illegal placements
        for (WorldServer worldServer : MinecraftServer.getServer().worldServers) {
            for (EntityPlayer playerEntity : worldServer.playerEntities) {
                if (playerEntity.getUniqueID() == Minecraft.getMinecraft().thePlayer.getUniqueID()) {
                    EntityPlayerMP playerEntity1 = (EntityPlayerMP) playerEntity;
                    playerEntity1.theItemInWorldManager.setBlockReachDistance(1000000);
                    playerEntity1.playerNetServerHandler.hasMoved = true;
                }
            }
        }
    }

    private void tickAI() { //Plant tree where it should be
        if (!ENABLE_DANGEROUS_STUFF) return;
        if (!running) return;
        if (currentTree == TREE_GOAL) {
            return;
        }
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
        if (thePlayer == null) return;

        Pos playerPos = getPos(new BigDecimal(currentTick));


        double treeLevel = 4;
        updateState();
        if (currentTree % 10000 == 0) {
            System.out.println("currentTree = " + currentTree);
        }
        if (currentTick % 10000 == 0) {
            System.out.println("currentTick = " + currentTick);

        }
        if (++index % 2 == 0) {
            BlockPos hitPos = new BlockPos(playerPos.x, treeLevel, playerPos.z);
            Minecraft.getMinecraft().playerController.onPlayerRightClick(thePlayer, Minecraft.getMinecraft().theWorld, thePlayer.getHeldItem(), hitPos, EnumFacing.UP, new Vec3(0, 0, 0));
            Minecraft.getMinecraft().thePlayer.swingItem();
            currentTree++;
        }

    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && Minecraft.getMinecraft().thePlayer != null && running) {
            Pos playerPos = getPos(new BigDecimal(currentTick).add(new BigDecimal(event.renderTickTime))); //Big decimal for high magnitude and precision


            //Math
            double playerHeight = 50;
            double horizOffset = 50;
            double treeLevel = 4;
            double x = playerPos.x;
            double z = playerPos.z;
            double angle = Math.atan(z / x);
            x -= Math.cos(x > 0 ? angle + Math.PI : angle) * horizOffset;
            z -= Math.sin(x > 0 ? angle + Math.PI : angle) * horizOffset;
            angle = Math.toDegrees(angle);
            angle -= 90;
            if (x > 0) {
                angle += 180;
            }
            Minecraft.getMinecraft().thePlayer.setLocationAndAngles(x, playerHeight, z, (float) angle, (float) Math.toDegrees(Math.atan(((playerHeight - treeLevel) / horizOffset))));
        }
        NumberFormat myFormat = NumberFormat.getInstance();
        myFormat.setGroupingUsed(true);

        String text = EnumChatFormatting.AQUA + "Trees Planted: " + myFormat.format(getSaplingCount());

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
            text = EnumChatFormatting.AQUA + "Progress: " + (myFormat.format(currentTree)) + "/" + myFormat.format(TREE_GOAL);
            stringWidth = fontRendererObj.getStringWidth(text);
            render(text, y, xTail, padding, fontRendererObj, stringWidth, scaledResolution);

            y += 10;
            text = EnumChatFormatting.AQUA.toString() + Math.round(currentTree * 100000D / ((double) TREE_GOAL)) / 1000D + "%";
            render(text, y, xTail, padding, fontRendererObj, fontRendererObj.getStringWidth(text), scaledResolution);


            y += 10;
            long ms = currentTree * 100;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'Hours:' H 'Minutes:' m 'Seconds:' s");
            boolean flag = ms > TimeUnit.DAYS.toMillis(1);
            if (flag) {
                ms -= TimeUnit.DAYS.toMillis(1);
                simpleDateFormat = new SimpleDateFormat("'Days:' d 'Hours:' H 'Minutes:' m 'Seconds:' s");
            }

            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            text = EnumChatFormatting.AQUA + (!flag ? "Days: 0 " : "") + simpleDateFormat.format(new Date(ms));

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pos pos = (Pos) o;
            return Double.compare(pos.x, x) == 0 &&
                    Double.compare(pos.z, z) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }
}
