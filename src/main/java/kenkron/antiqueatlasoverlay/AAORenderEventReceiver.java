package kenkron.antiqueatlasoverlay;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.RegistrarAntiqueAtlas;
import hunternif.mc.atlas.SettingsConfig;
import hunternif.mc.atlas.client.*;
import hunternif.mc.atlas.client.gui.GuiAtlas;
import hunternif.mc.atlas.core.DimensionData;
import hunternif.mc.atlas.map.objects.marker.DimensionMarkersData;
import hunternif.mc.atlas.map.objects.marker.Marker;
import hunternif.mc.atlas.map.objects.marker.MarkersData;
import hunternif.mc.atlas.map.objects.path.DimensionPathsData;
import hunternif.mc.atlas.map.objects.path.Path;
import hunternif.mc.atlas.map.objects.path.Segment;
import hunternif.mc.atlas.registry.MarkerRegistry;
import hunternif.mc.atlas.registry.MarkerRenderInfo;
import hunternif.mc.atlas.registry.MarkerType;
import hunternif.mc.atlas.util.AtlasRenderHelper;
import hunternif.mc.atlas.util.Rect;
import hunternif.mc.atlas.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;

@Mod.EventBusSubscriber(modid = AntiqueAtlasOverlayMod.MODID)
public class AAORenderEventReceiver {
    /**
     * Number of blocks per chunk in minecraft. This is certianly stored
     * somewhere else, but I couldn't be bothered to find it.
     */
    private static final int CHUNK_SIZE = 16;

    /**
     * new ScaledResolution(mc).getScaleFactor();
     */
    private static int screenScale = 1;

    public static ScaledResolution res;

    /**
     * Convenience method that returns the first atlas ID for all atlas items
     * the player is currently carrying in the hotbar/offhand. Returns null if
     * there are none. Offhand gets priority.
     **/
    public static Integer getPlayerAtlas(EntityPlayer player) {
        if (!SettingsConfig.gameplay.itemNeeded) {
            return player.getUniqueID().hashCode();
        }

        ItemStack stack = player.getHeldItemOffhand();
        if (!stack.isEmpty() && stack.getItem() == RegistrarAntiqueAtlas.ATLAS) {
            return stack.getItemDamage();
        }

        for (int i = 0; i < 9; i++) {
            stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == RegistrarAntiqueAtlas.ATLAS) {
                return stack.getItemDamage();
            }
        }

        return null;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (!AAOConfig.appearance.enabled) {
            return;
        }

        // Overlay must close if Atlas GUI is opened
        if (Minecraft.getMinecraft().currentScreen instanceof GuiAtlas) {
            return;
        }

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        Integer atlas = null;

        ItemStack stack = player.getHeldItemMainhand();
        ItemStack stack2 = player.getHeldItemOffhand();

        if (AAOConfig.appearance.requiresHold) {
            if (stack.getItem() == RegistrarAntiqueAtlas.ATLAS)
                atlas = stack.getItemDamage();
            else if (stack2.getItem() == RegistrarAntiqueAtlas.ATLAS)
                atlas = stack2.getItemDamage();

        } else if (stack.getItem() != RegistrarAntiqueAtlas.ATLAS && stack2.getItem() != RegistrarAntiqueAtlas.ATLAS || !SettingsConfig.userInterface.enableBookRender) {
            atlas = getPlayerAtlas(player);
        }

        if (atlas != null) {
            int gameWidth = event.getResolution().getScaledWidth();
            int gameHeight = event.getResolution().getScaledHeight();
            // remember, y=0 is at the top
            Rect bounds = new Rect().setOrigin(AAOConfig.position.xPosition, AAOConfig.position.yPosition);
            if (AAOConfig.position.alignRight) {
                bounds.minX = gameWidth - (AAOConfig.position.width + AAOConfig.position.xPosition);
            }
            if (AAOConfig.position.alignBottom) {
                bounds.minY = gameHeight - (AAOConfig.position.height + AAOConfig.position.yPosition);
            }

            bounds.setSize(AAOConfig.position.width, AAOConfig.position.height);
            res = event.getResolution();
            drawMinimap(bounds, atlas, player.getPositionEyes(event.getPartialTicks()), player.getRotationYawHead(), player.dimension);
        }
    }

    public static void drawMinimap(Rect shape, int atlasID, Vec3d playerPos, float rotation, int dimension) {
        screenScale = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0); // So light detail on tiles is
        // visible
        AtlasRenderHelper.drawFullTexture(Textures.BOOK, shape.minX, shape.minY, shape.getWidth(), shape.getHeight());
        Rect innerShape = new Rect(
                // stop it eclipse
                shape.minX + Math.round(AAOConfig.appearance.borderX * shape.getWidth()),
                shape.minY + Math.round(AAOConfig.appearance.borderY * shape.getHeight()),
                shape.maxX - Math.round(AAOConfig.appearance.borderX * shape.getWidth()),
                shape.maxY - Math.round(AAOConfig.appearance.borderY * shape.getHeight()));
        drawTiles(innerShape, atlasID, playerPos, dimension);

        if (AAOConfig.appearance.markerSize > 0) {
            drawMarkers(innerShape, atlasID, playerPos, dimension);
            int shapeMiddleX = (shape.minX + shape.maxX) / 2;
            int shapeMiddleY = (shape.minY + shape.maxY) / 2;
            drawPlayer(shapeMiddleX, shapeMiddleY, rotation);
        }

        drawPaths(innerShape, atlasID, playerPos, dimension);

        // Overlay the frame so that edges of the map are smooth:
        GlStateManager.color(1, 1, 1, 1);
        AtlasRenderHelper.drawFullTexture(Textures.BOOK_FRAME, shape.minX, shape.minY, shape.getWidth(), shape.getHeight());
        GlStateManager.disableBlend();

        int cx = (shape.maxX + shape.minX) / 2;
        int y = shape.maxY;
        Minecraft mc = Minecraft.getMinecraft();
        int px = MathHelper.floor(mc.player.posX + 0.5);
        int py = MathHelper.floor(mc.player.posY + 0.5);
        int pz = MathHelper.floor(mc.player.posZ + 0.5);
        drawCenteredLabel("" + px + ", " + py + ", " + pz, cx, y + 5);
    }

    private static void drawCenteredLabel(String line, int cx, int y) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;

        int stringWidth = fontRenderer.getStringWidth(line);
        int x = cx - stringWidth / 2;

        GlStateManager.scale(0.5, 0.5, 1);
        RenderUtil.draw9GridScaleTexture((x - 5) * 2, (y - 3 - 1) * 2, (stringWidth + 9) * 2, 30,
                new ResourceLocation("antiqueatlas", "textures/gui/book2.png"));
        GlStateManager.scale(2, 2, 1);

        fontRenderer.drawStringWithShadow(line, x, y, 0xffffffff);
    }

    private static void drawTiles(Rect shape, int atlasID, Vec3d playerPos,
                                  int dimension) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        // glScissor uses the default window coordinates,
        // the display window does not. We need to fix this
        glScissorGUI(shape);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        DimensionData biomeData = AntiqueAtlasMod.atlasData.getAtlasData(atlasID, Minecraft.getMinecraft().world).getDimensionData(dimension);

        TileRenderIterator iter = new TileRenderIterator(biomeData);
        Rect iteratorScope = getChunkCoverage(playerPos, shape);
        iter.setScope(iteratorScope);

        iter.setStep(1);
        Vec3d chunkPosition = new Vec3d(
                playerPos.x / CHUNK_SIZE,
                playerPos.y / CHUNK_SIZE,
                playerPos.z / CHUNK_SIZE
        );
        double shapeMiddleX = (shape.minX + shape.maxX) / 2d;
        double shapeMiddleY = (shape.minY + shape.maxY) / 2d;
        SetTileRenderer renderer = new SetTileRenderer(AAOConfig.appearance.tileSize / 2);

        while (iter.hasNext()) {
            SubTileQuartet subtiles = iter.next();
            for (SubTile subtile : subtiles) {
                if (subtile == null || subtile.tile == null)
                    continue;
                // Position of this subtile (measured in chunks) relative to the
                // player
                //          shapeMiddleX - AAOConfig.appearance.markerSize / 2d + AAOConfig.appearance.tileSize * (2 * (x / 2d) - 2 * Math.floor(playerPos.x / 2d)) / CHUNK_SIZE
                double xx = shapeMiddleX + (subtile.x / 2d + iteratorScope.minX - chunkPosition.x) * AAOConfig.appearance.tileSize;
                double yy = shapeMiddleY + (subtile.y / 2d + iteratorScope.minY - chunkPosition.z) * AAOConfig.appearance.tileSize;

                renderer.addTileCorner(
                        BiomeTextureMap.instance().getTexture(subtile.tile),
                        xx,
                        yy,
                        subtile.getTextureU(),
                        subtile.getTextureV()
                );
            }
        }
        renderer.draw();
        // get GL back to normal
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GlStateManager.color(1, 1, 1, 1);
    }

    private static void drawPaths(Rect shape, int atlasID, Vec3d playerPos, int dimension) {

        DimensionPathsData dimensionPathsData = AntiqueAtlasMod.pathsData.getOrCreate(atlasID, Minecraft.getMinecraft().world).get(dimension);

        Rect mcchunks = getChunkCoverage(playerPos, shape);
        Rect chunks = new Rect((int) Math.floor(mcchunks.minX / MarkersData.CHUNK_STEP),
                (int) Math.floor(mcchunks.minY / MarkersData.CHUNK_STEP),
                (int) Math.ceil(mcchunks.maxX / MarkersData.CHUNK_STEP),
                (int) Math.ceil(mcchunks.maxY / MarkersData.CHUNK_STEP));

        double shapeMiddleX = (shape.minX + shape.maxX) / 2d;
        double shapeMiddleY = (shape.minY + shape.maxY) / 2d;

        Set<Path> paths = new HashSet<>();
        for (int x = chunks.minX; x <= chunks.maxX; x++) {
            for (int z = chunks.minY; z <= chunks.maxY; z++) {
                paths.addAll(dimensionPathsData.getPathsInChunk(x, z));
            }
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        glScissorGUI(shape);
        GlStateManager.disableTexture2D();
        glEnable(GL_LINE_SMOOTH);
        GlStateManager.glLineWidth((float) 1);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (Path path : paths) {

            float red = ((path.color >> 16) & 0xFF) / 255f;
            float green = ((path.color >> 8) & 0xFF) / 255f;
            float blue = (path.color & 0xFF) / 255f;

            int x = path.startX;
            int z = path.startZ;

            buffer.pos(worldXToScreenXCentered(playerPos, shapeMiddleX, x), worldZToScreenYCentered(playerPos, shapeMiddleY, z), 0).color(0, 0, 0, 0).endVertex();

            for (short segment : path.segments) {
                Vec3i vector = Segment.getVector(segment);
                if (vector == null)
                    break;
                x += vector.getX();
                z += vector.getZ();
                buffer.pos(worldXToScreenXCentered(playerPos, shapeMiddleX, x), worldZToScreenYCentered(playerPos, shapeMiddleY, z), 0).color(red, green, blue, 0.8f).endVertex();
            }
        }

        Tessellator.getInstance().draw();

        glDisable(GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.color(1, 1, 1, 1);
    }

    private static double worldZToScreenYCentered(Vec3d playerPos, double shapeMiddleY, int z) {
        return worldZToScreenY(playerPos, shapeMiddleY, z) + AAOConfig.appearance.markerSize / 2d;
    }

    private static double worldXToScreenXCentered(Vec3d playerPos, double shapeMiddleX, int x) {
        return worldXToScreenX(playerPos, shapeMiddleX, x) + AAOConfig.appearance.markerSize / 2d;
    }

    private static double worldZToScreenY(Vec3d playerPos, double shapeMiddleY, double z) {
        return shapeMiddleY - AAOConfig.appearance.markerSize / 2d + AAOConfig.appearance.tileSize * (z - playerPos.z) / CHUNK_SIZE;
    }

    private static double worldXToScreenX(Vec3d playerPos, double shapeMiddleX, double x) {
        return shapeMiddleX - AAOConfig.appearance.markerSize / 2d + AAOConfig.appearance.tileSize * (x - playerPos.x) / CHUNK_SIZE;
    }


    private static void drawMarkers(Rect shape, int atlasID, Vec3d playerPos, int dimension) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        // glScissor uses the default window coordinates,
        // the display window does not. We need to fix this
        glScissorGUI(shape);

        // biomeData needed to prevent undiscovered markers from appearing
        DimensionData biomeData = AntiqueAtlasMod.atlasData.getAtlasData(
                atlasID, Minecraft.getMinecraft().world).getDimensionData(
                dimension);
        DimensionMarkersData globalMarkersData = AntiqueAtlasMod.globalMarkersData
                .getData().getMarkersDataInDimension(dimension);

        // Draw global markers:
        drawMarkersData(globalMarkersData, shape, biomeData, playerPos);

        MarkersData markersData = AntiqueAtlasMod.markersData.getMarkersData(
                atlasID, Minecraft.getMinecraft().world);
        DimensionMarkersData localMarkersData = null;
        if (markersData != null) {
            localMarkersData = markersData.getMarkersDataInDimension(dimension);
        }

        // Draw local markers:
        drawMarkersData(localMarkersData, shape, biomeData, playerPos);

        // get GL back to normal
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GlStateManager.color(1, 1, 1, 1);
    }

    private static void drawPlayer(float x, float y, float rotation) {
        // Draw player icon:

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.rotate(180 + rotation, 0, 0, 1);
        GlStateManager.translate(-AAOConfig.appearance.playerIconWidth / 2, -AAOConfig.appearance.playerIconHeight / 2, 0);
        AtlasRenderHelper.drawFullTexture(Textures.PLAYER, 0, 0, AAOConfig.appearance.playerIconWidth, AAOConfig.appearance.playerIconHeight);
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
    }

    private static void drawMarkersData(DimensionMarkersData markersData,
                                        Rect shape, DimensionData biomeData, Vec3d playerPos) {

        //this will be large enough to include markers that are larger than tiles
        Rect markerShape = new Rect(shape.minX - AAOConfig.appearance.markerSize / 2, shape.minY - AAOConfig.appearance.markerSize / 2,
                shape.maxX + AAOConfig.appearance.markerSize / 2, shape.maxY + AAOConfig.appearance.markerSize / 2);

        Rect mcchunks = getChunkCoverage(playerPos, markerShape);
        Rect chunks = new Rect((int) Math.floor(mcchunks.minX / MarkersData.CHUNK_STEP),
                (int) Math.floor(mcchunks.minY / MarkersData.CHUNK_STEP),
                (int) Math.ceil(mcchunks.maxX / MarkersData.CHUNK_STEP),
                (int) Math.ceil(mcchunks.maxY / MarkersData.CHUNK_STEP));

        double shapeMiddleX = (shape.minX + shape.maxX) / 2d;
        double shapeMiddleY = (shape.minY + shape.maxY) / 2d;

        for (int x = chunks.minX; x <= chunks.maxX; x++) {
            for (int z = chunks.minY; z <= chunks.maxY; z++) {
                List<Marker> markers = markersData.getMarkersAtChunk(x, z);
                if (markers == null)
                    continue;
                for (Marker marker : markers) {
                    renderMarker(marker, worldXToScreenX(playerPos, shapeMiddleX, marker.getX()), worldZToScreenY(playerPos, shapeMiddleY, marker.getZ()), biomeData);
                }
            }
        }
    }

    private static void renderMarker(Marker marker, double x, double y, DimensionData biomeData) {
        if (!marker.isVisibleAhead() && (biomeData == null || !biomeData.hasTileAt(marker.getChunkX(), marker.getChunkZ()))) {
            return;
        }
        GlStateManager.color(1, 1, 1, 1);
        MarkerType m = MarkerRegistry.find(marker.getTypeForRender());
        if (m == null) {
            return;
        }
        MarkerRenderInfo info = m.getRenderInfo(1, AAOConfig.appearance.tileSize, screenScale);


        AtlasRenderHelper.drawFullTexture(info.tex, x, y, AAOConfig.appearance.markerSize, AAOConfig.appearance.markerSize);
    }

    private static Rect getChunkCoverage(Vec3d position, Rect windowShape) {
        int minChunkX = (int) Math.floor(position.x / CHUNK_SIZE
                - windowShape.getWidth() / (2f * AAOConfig.appearance.tileSize));
        minChunkX -= 1;// IDK
        int minChunkY = (int) Math.floor(position.z / CHUNK_SIZE
                - windowShape.getHeight() / (2f * AAOConfig.appearance.tileSize));
        minChunkY -= 1;// IDK
        int maxChunkX = (int) Math.ceil(position.x / CHUNK_SIZE
                + windowShape.getWidth() / (2f * AAOConfig.appearance.tileSize));
        maxChunkX += 1;
        int maxChunkY = (int) Math.ceil(position.z / CHUNK_SIZE
                + windowShape.getHeight() / (2f * AAOConfig.appearance.tileSize));
        maxChunkY += 1;
        return new Rect(minChunkX, minChunkY, maxChunkX, maxChunkY);
    }

    public static boolean isBook = false;
    public static int bookFramebufferWidth = 1;
    public static int bookFramebufferHeight = 1;
    public static int bookGuiWidth = 1;
    public static int bookGuiHeight = 1;

    /**
     * Calls GL11.glScissor, but uses GUI coordinates
     */
    private static void glScissorGUI(Rect shape) {
        if (isBook) {
            float scissorScaleX = bookFramebufferWidth / (float) bookGuiWidth;
            float scissorScaleY = bookFramebufferHeight / (float) bookGuiHeight;
            int x = Math.max(0, (int) Math.floor(shape.minX * scissorScaleX));
            int y = Math.max(0, (int) Math.floor(bookFramebufferHeight - shape.maxY * scissorScaleY));
            int maxX = Math.min(bookFramebufferWidth, (int) Math.ceil(shape.maxX * scissorScaleX));
            int maxY = Math.min(bookFramebufferHeight, (int) Math.ceil(bookFramebufferHeight - shape.minY * scissorScaleY));
            GL11.glScissor(x, y, Math.max(0, maxX - x), Math.max(0, maxY - y));
            return;
        }
        // glScissor uses the default window coordinates,
        // the display window does not. We need to fix this
        int mcHeight = Minecraft.getMinecraft().displayHeight;
        float scissorScaleX = Minecraft.getMinecraft().displayWidth * 1.0f
                / res.getScaledWidth();
        float scissorScaleY = mcHeight * 1.0f / res.getScaledHeight();
        GL11.glScissor((int) (shape.minX * scissorScaleX),
                (int) (mcHeight - shape.maxY * scissorScaleY),
                (int) (shape.getWidth() * scissorScaleX),
                (int) (shape.getHeight() * scissorScaleY));
    }
}
