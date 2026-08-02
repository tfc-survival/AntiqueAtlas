package hunternif.mc.atlas.client.ingame.book;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

public class AtlasBakedModel implements IBakedModel {
    private IBakedModel inventory;
    private IBakedModel builtin;

    public AtlasBakedModel(IBakedModel inventory) {
        this.inventory = inventory;
        builtin = new BuiltInModel(ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return ImmutableList.of();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return inventory.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return inventory.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return inventory.getOverrides();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        BookRenderer.cameraTransformType = cameraTransformType;
        switch (cameraTransformType) {
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
            case GROUND:
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
            case HEAD:
                return builtin.handlePerspective(cameraTransformType);
            default:
                return inventory.handlePerspective(cameraTransformType);
        }
    }
}
