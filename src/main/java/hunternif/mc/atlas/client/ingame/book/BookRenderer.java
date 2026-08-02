package hunternif.mc.atlas.client.ingame.book;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.client.IngameScaleToggler;
import hunternif.mc.atlas.util.Rect;
import kenkron.antiqueatlasoverlay.AAORenderEventReceiver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static hunternif.mc.atlas.client.ingame.book.BookRenderer.PageFlipState.*;
import static hunternif.mc.atlas.client.ingame.book.BookRenderer.RenderCase.*;
import static hunternif.mc.atlas.client.ingame.book.BookRenderer.SwingState.*;
import static hunternif.mc.atlas.client.ingame.book.Refs.*;
import static net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.*;

@SideOnly(Side.CLIENT)
public class BookRenderer extends TileEntityItemStackRenderer {

    public static ItemCameraTransforms.TransformType cameraTransformType = FIRST_PERSON_LEFT_HAND;

    ResourceLocation cover = new ResourceLocation(AntiqueAtlasMod.ID, "textures/models/book/em_book2.png");
    ResourceLocation pages = new ResourceLocation(AntiqueAtlasMod.ID, "textures/models/book/page.png");
    BookModel model = new BookModel();

    private Framebuffer pageTexture, nextPageTexture;

    private void initFBO() {
        if (pageTexture == null) {
            pageTexture = new Framebuffer(pageContainerWidth, pageHeight, false);
            nextPageTexture = new Framebuffer(pageContainerWidth, pageHeight, false);
        }
    }

    public enum RenderCase {
        right, left, both, other
    }

    public enum SwingState {
        openingStart, openingEnd, closing
    }

    private static SwingState leftSwingState = openingEnd;
    private static SwingState rightSwingState = openingEnd;
    private static double prevF5 = 0;
    private static double prevF6 = 0;

    public enum PageFlipState {
        flipLeft, flipRight, normal;
    }

    private static PageFlipState pageFlipState = normal;

    private static int flipProgress = 0;

    private static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;


    @Override
    public void renderByItem(ItemStack itemStackIn, float partialTicks) {
        initFBO();
        partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();

        ItemRenderer itemRenderer = Minecraft.getMinecraft().getItemRenderer();

        EnumHand enumhand = cameraTransformType == FIRST_PERSON_LEFT_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        float f = player.getSwingProgress(partialTicks);

        RenderCase renderCase;
        if (cameraTransformType == FIRST_PERSON_LEFT_HAND) {
            renderCase = left;
        } else if (cameraTransformType == FIRST_PERSON_RIGHT_HAND) {
            if (player.getHeldItemOffhand().isEmpty())
                renderCase = both;
            else
                renderCase = right;
        } else
            renderCase = other;

        float f3 = enumhand == EnumHand.MAIN_HAND ? f : 0.0F;
        float f5 = 1.0F - (itemRenderer.prevEquippedProgressMainHand + (itemRenderer.equippedProgressMainHand - itemRenderer.prevEquippedProgressMainHand) * partialTicks);
        float f4 = enumhand == EnumHand.OFF_HAND ? f : 0.0F;
        float f6 = 1.0F - (itemRenderer.prevEquippedProgressOffHand + (itemRenderer.equippedProgressOffHand - itemRenderer.prevEquippedProgressOffHand) * partialTicks);

        // System.out.println(progress2);
        /*

        GlStateManager.pushMatrix();

        float f1 = renderCase == right ? 1.0F : -1.0F;
        int i = cameraTransformType == FIRST_PERSON_RIGHT_HAND ? 1 : -1;

        GlStateManager.scale(2, 2, 2);
        switch (renderCase) {
            case right:
                //GlStateManager.rotate(f * 10.0F, 0.0F, 0.0F, 1.0F);
                itemRenderer.renderArmFirstPerson(f3, f5, EnumHandSide.RIGHT);
                break;
            case left:
                GlStateManager.translate(-1 * (float) i * 0.56F, -1 * (-0.52F + f6 * -0.6F), -1 * -0.72F);
                GlStateManager.translate(f1 * 0.125F, 0, 0.0F);
                //GlStateManager.rotate(f * 10.0F, 0.0F, 0.0F, 1.0F);
                itemRenderer.renderArmFirstPerson(f4, f6, EnumHandSide.LEFT);
                break;
            case both:
                itemRenderer.renderArms();
                break;
        }
        GlStateManager.popMatrix();

        */

        int atlas = itemStackIn.getItemDamage();

        float scale = 1f / 16;

        double x = 0.5 + (renderCase == right ? 0.2 : renderCase == left ? -0.2 : renderCase == both ? -0.55 : 0);
        double y = 0.5 + (renderCase == both ? 0.5 : 0);
        double z = 0;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-90, 0, 1, 0);
        if (renderCase == both) {
            double s = 1.5;
            GlStateManager.scale(s, s, s);
            float angle = getMapAngleFromPitch(player.rotationPitch);
            GlStateManager.translate(0, 0.04F + f5 * -1.2F + angle * -0.5F, 0);
            GlStateManager.rotate(angle * -85.0F, 0, 0, -1);
        }
        GlStateManager.enableTexture2D();

        double progress2 = 0;

        if (renderCase == right || renderCase == both || cameraTransformType == THIRD_PERSON_RIGHT_HAND) {
            if (rightSwingState == openingStart) {
                progress2 = Math.PI + Math.PI / 2;
                if (prevF5 > f5)
                    rightSwingState = openingEnd;
            } else if (rightSwingState == openingEnd) {
                progress2 = Math.PI + Math.PI / 2 - (1 - f5) * Math.PI;
                if (prevF5 < f5)
                    rightSwingState = closing;
            } else if (rightSwingState == closing) {
                progress2 = Math.PI + Math.PI / 2 - (1 - Math.min(1, (f5 * 2))) * Math.PI;
                if (f5 > 0.79)
                    rightSwingState = openingStart;
            }
        } else if (renderCase == left || cameraTransformType == THIRD_PERSON_LEFT_HAND) {
            if (leftSwingState == openingStart) {
                progress2 = Math.PI + Math.PI / 2;
                if (prevF6 > f6)
                    leftSwingState = openingEnd;
            } else if (leftSwingState == openingEnd) {
                progress2 = Math.PI + Math.PI / 2 - (1 - f6) * Math.PI;
                if (prevF6 < f6)
                    leftSwingState = closing;
            } else if (leftSwingState == closing) {
                progress2 = Math.PI + Math.PI / 2 - (1 - Math.min(1, (f6 * 2))) * Math.PI;
                if (f6 > 0.79)
                    leftSwingState = openingStart;
            }
        } else if (renderCase == other)
            progress2 = Math.PI + Math.PI / 2;
        prevF5 = f5;
        prevF6 = f6;

        double progress = progress2;//(double) System.currentTimeMillis() / 100;
        float openProgress = (float) ((Math.sin(progress) + 1) / 2);

        if (cameraTransformType == THIRD_PERSON_LEFT_HAND || cameraTransformType == THIRD_PERSON_RIGHT_HAND) {
            GlStateManager.translate(0.55, 0.155, 0);
            double s = 0.5;
            GlStateManager.scale(s, s, s);
        } else if (cameraTransformType == GROUND) {
            GlStateManager.translate(0.3, 0.1, 0);
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(cover);
        GlStateManager.enableCull();
        model.renderCover((float) progress, openProgress, 0, scale);


        int maxFlipTicks = 20;

        model.setRotationAnglesPages((float) progress,
                (float) (pageFlipState == normal ? 0 :
                        pageFlipState == flipRight ?
                                flipProgress + (1 - partialTicks) :
                                flipProgress + partialTicks) / maxFlipTicks,
                openProgress);

        if (IngameScaleToggler.scaleChanged) {
            IngameScaleToggler.scaleChanged = false;
            pageFlipState = IngameScaleToggler.scaleChangeDir == 1 ? flipRight : flipLeft;
            flipProgress = IngameScaleToggler.scaleChangeDir == 1 ? maxFlipTicks : 0;
        }

        if (pageFlipState == normal) {
            drawPageTo(pageTexture, atlas);
            model.renderPageContainerLeft(pageTexture, pageTexture, scale);
            model.renderPageContainerRight(pageTexture, pageTexture, scale);

        } else if (pageFlipState == flipRight) {
            drawPageTo(nextPageTexture, atlas);
            model.renderPageContainerLeft(pageTexture, nextPageTexture, scale);
            model.renderPageContainerRight(nextPageTexture, pageTexture, scale);

            flipProgress--;
            if (flipProgress <= 0) {
                pageFlipState = normal;
                Framebuffer t = pageTexture;
                pageTexture = nextPageTexture;
                nextPageTexture = t;
            }
        } else if (pageFlipState == flipLeft) {
            drawPageTo(nextPageTexture, atlas);
            model.renderPageContainerLeft(nextPageTexture, pageTexture, scale);
            model.renderPageContainerRight(pageTexture, nextPageTexture, scale);

            flipProgress++;
            if (flipProgress >= maxFlipTicks) {
                pageFlipState = normal;
                Framebuffer t = pageTexture;
                pageTexture = nextPageTexture;
                nextPageTexture = t;
            }
        }

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.bindTexture(0);
        GlStateManager.popMatrix();
    }

    private float getMapAngleFromPitch(float pitch) {
        float f = 1.0F - pitch / 45.0F + 0.1F;
        f = MathHelper.clamp(f, 0.0F, 1.0F);
        f = -MathHelper.cos(f * (float) Math.PI) * 0.5F + 0.5F;
        return f;
    }

    private void drawPageTo(Framebuffer framebuffer, int atlas) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderTargetState renderTargetState = RenderTargetState.capture();
        ScaledResolution oldResolution = AAORenderEventReceiver.res;
        boolean wasBook = AAORenderEventReceiver.isBook;
        int oldBookFramebufferWidth = AAORenderEventReceiver.bookFramebufferWidth;
        int oldBookFramebufferHeight = AAORenderEventReceiver.bookFramebufferHeight;
        int oldBookGuiWidth = AAORenderEventReceiver.bookGuiWidth;
        int oldBookGuiHeight = AAORenderEventReceiver.bookGuiHeight;

        try {
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.disableDepth();
            GlStateManager.disableFog();
            GlStateManager.color(1, 1, 1, 1);

            framebuffer.bindFramebuffer(true);

            saveMatrices();
            try {
                identityMatrices();
                setupProjectionArea();

                GL11.glMatrixMode(GL11.GL_MODELVIEW);

                GlStateManager.clearColor(1, 1, 1, 1);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
                GlStateManager.translate(0, 0, -2000);

                EntityPlayer player = mc.player;
                AAORenderEventReceiver.res = new ScaledResolution(mc);
                AAORenderEventReceiver.isBook = true;
                AAORenderEventReceiver.bookFramebufferWidth = pageContainerWidth;
                AAORenderEventReceiver.bookFramebufferHeight = pageHeight;
                AAORenderEventReceiver.bookGuiWidth = w;
                AAORenderEventReceiver.bookGuiHeight = h;
                AAORenderEventReceiver.drawMinimap(
                        new Rect(-19, -17, 245, 199),
                        atlas,
                        player.getPositionVector(),
                        player.getRotationYawHead(),
                        player.dimension
                );
            } finally {
                restoreMatrices();
            }
        } finally {
            AAORenderEventReceiver.res = oldResolution;
            AAORenderEventReceiver.isBook = wasBook;
            AAORenderEventReceiver.bookFramebufferWidth = oldBookFramebufferWidth;
            AAORenderEventReceiver.bookFramebufferHeight = oldBookFramebufferHeight;
            AAORenderEventReceiver.bookGuiWidth = oldBookGuiWidth;
            AAORenderEventReceiver.bookGuiHeight = oldBookGuiHeight;
            renderTargetState.restore();
            GlStateManager.enableDepth();
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    private static class RenderTargetState {
        private final int framebuffer;
        private final boolean scissorEnabled;
        private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
        private final IntBuffer scissorBox = BufferUtils.createIntBuffer(16);
        private final FloatBuffer clearColor = BufferUtils.createFloatBuffer(16);

        static RenderTargetState capture() {
            return new RenderTargetState();
        }

        private RenderTargetState() {
            framebuffer = OpenGlHelper.isFramebufferEnabled() ? GL11.glGetInteger(GL_FRAMEBUFFER_BINDING) : 0;
            scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, scissorBox);
            GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        }

        void restore() {
            if (OpenGlHelper.isFramebufferEnabled()) {
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
            }
            GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            if (scissorEnabled) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
            GL11.glScissor(scissorBox.get(0), scissorBox.get(1), scissorBox.get(2), scissorBox.get(3));
            GlStateManager.clearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
        }
    }

    public void setupProjectionArea() {
        GL11.glOrtho(0, w, h, 0, -300, 3000);
    }

    public void identityMatrices() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
    }

    public void saveMatrices() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
    }

    public void restoreMatrices() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }
}
