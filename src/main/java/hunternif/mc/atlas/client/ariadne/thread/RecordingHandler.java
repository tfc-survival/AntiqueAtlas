package hunternif.mc.atlas.client.ariadne.thread;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.item.ItemAriadneThread;
import hunternif.mc.atlas.map.objects.path.Segment;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.server.FlushAriadneThreadPoses;
import hunternif.mc.atlas.network.server.PacketStartPathRecording;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.security.SecureRandom;
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
    private static long recordingTarget = 0;
    private static List<Short> sendQueue = new ArrayList<>();

    private static BlockPos lastPos;

    public static boolean isActive() {
        return recordingTarget != 0;
    }

    private static class Holder {
        static final SecureRandom numberGenerator = new SecureRandom();
    }

    public static void start(ItemStack heldItem, EnumHand hand) {
        if (recordingTarget == 0) {
            recordingTarget = Holder.numberGenerator.nextLong();
            PacketDispatcher.sendToServer(new PacketStartPathRecording(hand, recordingTarget));
            ItemAriadneThread.activate(heldItem, recordingTarget);
            lastPos = RenderHandler.load(heldItem);
        }
    }

    public static void stop(ItemStack heldItem) {
        if (recordingTarget == ItemAriadneThread.getActiveKey(heldItem)) {
            addSegment();
            flush();
            RenderHandler.clear();
            recordingTarget = 0;
        }
    }

    public static void failStop() {
        sendQueue = new ArrayList<>();
        RenderHandler.clear();
        recordingTarget = 0;
    }


    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (recordingTarget != 0) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null) {
                failStop();
                return;
            }

            int activeItem = getActiveItemClient(player);
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
                if (current.distanceSq(lastPos) >= maxRecordingDistanceSq) {
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
            failStop();
            player.sendStatusMessage(new TextComponentTranslation("msg.too.far"), true);
            return false;
        }
    }

    public static void flush() {
        if (sendQueue.isEmpty())
            return;

        int activeItem = getActiveItemClient(Minecraft.getMinecraft().player);
        if (activeItem >= -1) {
            PacketDispatcher.sendToServer(new FlushAriadneThreadPoses(activeItem, sendQueue));
            sendQueue = new ArrayList<>();
        }
    }

    public static boolean isActiveStack(ItemStack stack) {
        long activeKey = ItemAriadneThread.getActiveKey(stack);
        return activeKey != 0 && recordingTarget == activeKey;
    }

    public static int getActiveItemClient(EntityPlayer player) {
        if (isActiveStack(player.getHeldItemMainhand()))
            return player.inventory.currentItem;

        if (isActiveStack(player.getHeldItemOffhand()))
            return 40;

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (isActiveStack(player.inventory.getStackInSlot(i))) {
                return i;
            }
        }

        if (isActiveStack(player.inventory.getItemStack()))
            return -1;

        return -2;
    }
}
