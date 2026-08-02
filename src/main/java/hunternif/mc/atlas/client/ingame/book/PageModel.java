package hunternif.mc.atlas.client.ingame.book;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class PageModel {
    public final boolean isLeft;

    public double x = 0;
    public double y = -4;
    public double z = 0;
    public double w = 5;
    public double h = 8;

    public double u1;
    public double u2;
    public double v1 = 0;
    public double v2 = 1;

    public double nz;

    public float rotateAngleY = 0;
    public float rotationPointX = 0;

    public PageModel(boolean isLeft) {
        this.isLeft = isLeft;
        u1 = this.isLeft ? 0 : 0.5;
        u2 = this.isLeft ? 0.5 : 1;
        nz = this.isLeft ? 1 : -1;
    }

    public void render(double scale) {

        GlStateManager.disableLighting();

        GlStateManager.pushMatrix();
        GlStateManager.translate(rotationPointX * scale, 0, 0);

        if (rotateAngleY != 0.0F)
            GlStateManager.rotate(rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);

        BufferWrapper buffer = new BufferWrapper(Tessellator.getInstance().getBuffer(), scale);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        if (isLeft) {
            buffer.pos(x, y + h, z).tex(u2, v2).color(255, 255, 255, 255).endVertex();
            buffer.pos(x + w, y + h, z).tex(u1, v2).color(255, 255, 255, 255).endVertex();
            buffer.pos(x + w, y, z).tex(u1, v1).color(255, 255, 255, 255).endVertex();
            buffer.pos(x, y, z).tex(u2, v1).color(255, 255, 255, 255).endVertex();

        } else {
            buffer.pos(x, y, z).tex(u1, v1).color(255, 255, 255, 255).endVertex();
            buffer.pos(x + w, y, z).tex(u2, v1).color(255, 255, 255, 255).endVertex();
            buffer.pos(x + w, y + h, z).tex(u2, v2).color(255, 255, 255, 255).endVertex();
            buffer.pos(x, y + h, z).tex(u1, v2).color(255, 255, 255, 255).endVertex();
        }

        Tessellator.getInstance().draw();

        GlStateManager.popMatrix();

        GlStateManager.enableLighting();

    }

}
