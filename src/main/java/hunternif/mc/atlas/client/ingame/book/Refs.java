package hunternif.mc.atlas.client.ingame.book;

import net.minecraft.client.Minecraft;

public class Refs {
    public static int resolutionMultiplier = 3;

    public static int backgroundPixelSize = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;

    public static int textureW = 25;
    public static int textureH = 20;

    public static int pageContainerWidth = textureW * backgroundPixelSize * 2 * resolutionMultiplier;
    public static int pageHeight = textureH * backgroundPixelSize * 2 * resolutionMultiplier;

    public static int w = pageContainerWidth / 2 / resolutionMultiplier;
    public static int h = pageHeight / 2 / resolutionMultiplier;

    public static int contentWidth = (textureW / 2 - 1) * backgroundPixelSize;
    public static int contentHeight = (textureH - 2) * backgroundPixelSize;

    public static int leftPageStartX = 1 * backgroundPixelSize;
    public static int rightPageStartX = (textureW / 2 + 1) * backgroundPixelSize;
}
