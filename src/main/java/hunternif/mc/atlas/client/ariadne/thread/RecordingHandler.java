package hunternif.mc.atlas.client.ariadne.thread;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.item.ItemAriadneThread;
import hunternif.mc.atlas.map.objects.path.Segment;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.server.FlushAriadneThreadPoses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.math.Vec3d.ZERO;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID, value = Side.CLIENT)
public class RecordingHandler {
    public static final long maxFlushPeriodMS = 1000 * 60;
    public static final double maxRecordingDistanceSq = (Segment.maxDistance - 2) * (Segment.maxDistance - 2);
    public static final double directionAngleValueableDifference = Math.toRadians(45 / 2);
    private static long lastFlushTime = 0;
    private static Vec3d direction;
    private static boolean recording = false;
    private static List<Short> sendQueue = new ArrayList<>();

    private static BlockPos lastPos;

    public static boolean isActive() {
        return recording;
    }

    public static void start(ItemStack heldItem) {
        if (!recording) {
            recording = true;
            lastPos = RenderHandler.load(heldItem);
        }
    }

    public static void stop() {
        if (recording) {
            addSegment();
            flush();
            RenderHandler.clear();
            recording = false;
        }
    }

    public static void failStop() {
        sendQueue = new ArrayList<>();
        RenderHandler.clear();
        recording = false;
    }


    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (recording) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null) {
                failStop();
                return;
            }

            int activeItem = ItemAriadneThread.getActiveItemClient(player);
            if (activeItem == -2) {
                failStop();
                return;
            }

            if (direction == null)
                direction = getCurrentDirection();

            if (diff(direction, getCurrentDirection()) > directionAngleValueableDifference) {
                direction = getCurrentDirection();
                addSegment();

            } else {
                BlockPos current = ItemAriadneThread.posOfPlayer(player);
                if (current.distanceSq(lastPos) >= 289) {
                    addSegment();
                }
            }

            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastFlushTime >= maxFlushPeriodMS || sendQueue.size() == ItemAriadneThread.maxQueueSize) {
                lastFlushTime = currentTimeMillis;
                flush();
            }
        }
    }

    private static double diff(Vec3d a, Vec3d b) {
        if (a == ZERO || b == ZERO)
            return directionAngleValueableDifference + 1;

        return Math.acos((a.x * b.x + a.y * b.y + a.z * b.z) / (a.length() * b.length()));
    }

    private static Vec3d getCurrentDirection() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        return new Vec3d(player.motionX, player.motionY, player.motionZ).normalize();
    }

    public static boolean addSegment() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        BlockPos pos = ItemAriadneThread.posOfPlayer(player);
        if (lastPos.equals(pos))
            return false;

        RenderHandler.addPos(pos);
        short index = Segment.getIndex(lastPos, pos);

        if (index >= 0) {

            BlockPos vector1 = pos.subtract(lastPos);
            BlockPos vector2 = new BlockPos(Segment.getVector(index));
            if (!vector1.equals(vector2))
                System.out.println("bruh, violated segment index, lastPos=" + lastPos + ", current=" + pos + ", index=" + index + ", vector1=" + vector1 + ", vector2=" + vector2);

            sendQueue.add(index);
            lastPos = pos;
            return true;
        } else {
            recording = false;
            RenderHandler.clear();
            player.sendStatusMessage(new TextComponentTranslation("msg.too.far"), true);
            return false;
        }
    }

    public static void flush() {
        if (sendQueue.isEmpty())
            return;

        int activeItem = ItemAriadneThread.getActiveItemClient(Minecraft.getMinecraft().player);
        if (activeItem >= -1) {
            PacketDispatcher.sendToServer(new FlushAriadneThreadPoses(activeItem, sendQueue));
            sendQueue = new ArrayList<>();
        }
    }
}
