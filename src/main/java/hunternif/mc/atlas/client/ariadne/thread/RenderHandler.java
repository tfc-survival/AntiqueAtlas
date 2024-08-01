package hunternif.mc.atlas.client.ariadne.thread;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.item.ItemAriadneThread;
import hunternif.mc.atlas.map.objects.path.Segment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ARIADNE_THREAD;
import static org.lwjgl.opengl.GL11.*;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID, value = Side.CLIENT)
public class RenderHandler {
    private static LinkedList<BlockPos> recordingPath = new LinkedList<>();
    private static List<BlockPos> heldItemPath = new ArrayList<>();

    private static ItemStack lastHoldItem = ItemStack.EMPTY;

    public static void addPos(BlockPos pos) {
        recordingPath.add(pos);
    }

    public static void clear() {
        recordingPath.clear();
    }

    public static BlockPos load(ItemStack stack) {
        return load(stack, recordingPath);
    }

    public static BlockPos load(ItemStack stack, List<BlockPos> consumer) {
        consumer.clear();
        BlockPos start = ItemAriadneThread.getStart(stack);
        short[] segments = ItemAriadneThread.getPath(stack);
        if (start != null && segments != null) {
            consumer.add(start);
            BlockPos lastPos = start;
            for (short index : segments) {
                Vec3i v = Segment.getVector(index);
                if (v == null) {
                    System.out.println("violated ball nbt " + stack + ", " + Arrays.toString(segments));
                    return onlyCurrent(consumer);
                }
                lastPos = lastPos.add(v);
                consumer.add(lastPos);
            }
            return lastPos;
        }

        return onlyCurrent(consumer);
    }

    private static BlockPos onlyCurrent(List<BlockPos> consumer) {
        BlockPos lastPos = ItemAriadneThread.posOfPlayer(Minecraft.getMinecraft().player);
        consumer.add(lastPos);
        return lastPos;
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        ItemStack current = getCurrentHeldItem();
        if (current.isEmpty() || ItemAriadneThread.isActive(current)) {
            if (RecordingHandler.isActive()) {
                renderLine(recordingPath, event.getPartialTicks(), getActiveItemStack());
            }
        } else {
            if (current != lastHoldItem) {
                lastHoldItem = current;
                load(current, heldItemPath);
            }
            if (heldItemPath.size() > 1)
                renderLine(heldItemPath, event.getPartialTicks(), lastHoldItem);
        }
    }

    private static ItemStack getActiveItemStack() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        int slot = ItemAriadneThread.getActiveItem(player);
        if (slot == -1)
            return player.inventory.getItemStack();

        else if (0 <= slot && slot < player.inventory.getSizeInventory())
            return player.inventory.getStackInSlot(slot);

        else
            return ItemStack.EMPTY;
    }

    private static ItemStack getCurrentHeldItem() {
        EntityPlayerSP self = Minecraft.getMinecraft().player;

        if (self.getHeldItemMainhand().getItem() == ARIADNE_THREAD)
            return self.getHeldItemMainhand();

        else if (self.getHeldItemOffhand().getItem() == ARIADNE_THREAD)
            return self.getHeldItemOffhand();

        else
            return ItemStack.EMPTY;
    }

    private static void renderLine(List<BlockPos> poses, float partialTicks, ItemStack ball) {
        int color = ARIADNE_THREAD.getColor(ball);
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color) & 0xFF;

        EntityPlayerSP self = Minecraft.getMinecraft().player;
        double x = self.lastTickPosX + (self.posX - self.lastTickPosX) * partialTicks;
        double y = self.lastTickPosY + (self.posY - self.lastTickPosY) * partialTicks;
        double z = self.lastTickPosZ + (self.posZ - self.lastTickPosZ) * partialTicks;
        setupGL();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (BlockPos p : poses) {
            buffer.pos(p.getX() + 0.5 - x, p.getY() + 0.5 - y, p.getZ() + 0.5 - z).color(red, green, blue, 200).endVertex();
        }

        buffer.pos(0, 0, 0).color(red, green, blue, 200).endVertex();

        Tessellator.getInstance().draw();

        resetGL();
    }

    private static void setupGL() {
        GlStateManager.disableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        GlStateManager.glLineWidth(2);
    }

    private static void resetGL() {
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        glDisable(GL_LINE_SMOOTH);
    }
}
