package hunternif.mc.atlas.client.ingame.book;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;

public class BookModel extends ModelBase {
    ModelRenderer coverRight = new ModelRenderer(this).setTextureOffset(0, 0).addBox(-6, -5, 0, 6, 10, 0);

    ModelRenderer coverLeft = new ModelRenderer(this).setTextureOffset(16, 0).addBox(0, -5, 0, 6, 10, 0);

    ModelRenderer pagesRight = new ModelRenderer(this).setTextureOffset(0, 10).addBox(0, -4, -0.99F, 5, 8, 1);

    ModelRenderer pagesLeft = new ModelRenderer(this).setTextureOffset(12, 10).addBox(0, -4, -0.01F, 5, 8, 1);

    PageModel flippingPageRight = new PageModel(false);
    PageModel flippingPageLeft = new PageModel(true);

    PageModel rightPage = new PageModel(false);
    PageModel leftPage = new PageModel(true);

    ModelRenderer bookSpine = new ModelRenderer(this).setTextureOffset(12, 0).addBox(-1, -5, 0, 2, 10, 0);

    {
        coverRight.setRotationPoint(0, 0, -1);
        coverLeft.setRotationPoint(0, 0, 1);
        bookSpine.rotateAngleY = (float) Math.PI / 2F;
    }

    public void renderCover(float limbSwing, float openProgress, float headPitch, float scale) {
        setRotationAnglesCover(limbSwing, openProgress, headPitch, scale);
        coverRight.render(scale);
        coverLeft.render(scale);
        bookSpine.render(scale);
        pagesRight.render(scale);
        pagesLeft.render(scale);
    }

    public void renderPageContainerRight(Framebuffer rightTexture, Framebuffer flippingRightTexture, double scale) {
        setupStandartAttributes();
        rightTexture.bindFramebufferTexture();
        rightPage.render(scale);
        flippingRightTexture.bindFramebufferTexture();
        flippingPageRight.render(scale);
        rightTexture.unbindFramebufferTexture();
    }

    public void renderPageContainerLeft(Framebuffer leftTexture, Framebuffer flippingLeftTexture,double scale) {
        setupStandartAttributes();
        leftTexture.bindFramebufferTexture();
        //if (Keyboard.isKeyDown(Keyboard.KEY_H))
        //    FBODumper.dumped = false;
        //FBODumper.dump();
        leftPage.render(scale);
        flippingLeftTexture.bindFramebufferTexture();
        flippingPageLeft.render(scale);
        leftTexture.unbindFramebufferTexture();
    }


    public void setupStandartAttributes() {
        GlStateManager.enableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
    }
/*
    public void renderPages(Entity entityIn, float limbSwing, float flipping, float netHeadYaw, float openProgress, double scale) {
        setRotationAnglesPages(limbSwing, flipping, netHeadYaw);

        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(AntiqueAtlasMod.ID, "textures/models/book/page.png"));
        rightPage.render(scale);
        flippingPageLeft.render(scale);

        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(AntiqueAtlasMod.ID, "textures/models/book/page2.png"));
        leftPage.render(scale);
        flippingPageRight.render(scale);
    }*/

    public void setRotationAnglesCover(float limbSwing, float openProgress, float headPitch, float scaleFactor) {
        float f = (MathHelper.sin(limbSwing * 0.02F) * 0.1F + 1.25F) * openProgress;
        coverRight.rotateAngleY = (float) Math.PI + f;
        coverLeft.rotateAngleY = -f;
        pagesRight.rotateAngleY = f;
        pagesLeft.rotateAngleY = -f;
        pagesRight.rotationPointX = MathHelper.sin(f) + 0.0015f;
        pagesRight.rotationPointZ = 0.0015f;
        pagesLeft.rotationPointX = MathHelper.sin(f) + 0.0015f;
        pagesLeft.rotationPointZ = -0.0015f;
    }

    public void setRotationAnglesPages(float limbSwing, float flipping, float openProgress) {
        float f = (MathHelper.sin(limbSwing * 0.02F) * 0.1F + 1.25F) * openProgress;

        rightPage.rotateAngleY = f;
        leftPage.rotateAngleY = -f;
        flippingPageRight.rotateAngleY = f - f * 2 * flipping;
        flippingPageLeft.rotateAngleY = f - f * 2 * flipping;

        rightPage.rotationPointX = MathHelper.sin(f);
        leftPage.rotationPointX = MathHelper.sin(f);
        flippingPageRight.rotationPointX = MathHelper.sin(f);
        flippingPageLeft.rotationPointX = MathHelper.sin(f);

    }
}
