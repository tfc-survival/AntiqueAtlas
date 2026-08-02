package hunternif.mc.atlas.map.objects.path;

import com.google.common.collect.ImmutableList;
import su.tfcsurvival.util.Vec2i;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class Segment {

    public static int maxDistance = 19;

    private static int dimensionSize = maxDistance * 2 + 1;

    private static short[][][] vectorToIndex = new short[dimensionSize][dimensionSize][dimensionSize];

    private static final List<Vec3i> indexToVector;

    public static final int maxIndex;

    static {
        short index = 0;
        ImmutableList.Builder<Vec3i> indexToVectorBuilder = ImmutableList.builder();
        for (int x = -maxDistance; x <= maxDistance; x++) {
            for (int z = -maxDistance; z <= maxDistance; z++) {
                for (int y = -maxDistance; y <= maxDistance; y++) {
                    if (x * x + z * z + y * y <= maxDistance * maxDistance) {
                        indexToVectorBuilder.add(new Vec3i(x, y, z));
                        vectorToIndex[x + maxDistance][y + maxDistance][z + maxDistance] = index;
                        index++;
                    } else
                        vectorToIndex[x + maxDistance][y + maxDistance][z + maxDistance] = -1;
                }
            }
        }
        indexToVector = indexToVectorBuilder.build();
        maxIndex = indexToVector.size() - 1;
    }

    public static short getIndex(BlockPos prev, BlockPos current) {
        BlockPos vector = current.subtract(prev);
        int x = vector.getX() + maxDistance;
        int y = vector.getY() + maxDistance;
        int z = vector.getZ() + maxDistance;
        if (0 <= x && x < dimensionSize &&
                0 <= y && y < dimensionSize &&
                0 <= z && z < dimensionSize)
            return vectorToIndex[x][y][z];
        else
            return -1;
    }

    public static Vec3i getVector(short index) {
        if (0 <= index && index < indexToVector.size())
            return indexToVector.get(index);
        else
            return null;
    }

    public static void main(String[] a) {

        {
            List<Vec3i> suitablePosesAround = new ArrayList<>(4000);
            int maxDistance = 19;
            for (int x = -maxDistance; x <= maxDistance; x++) {
                for (int z = -maxDistance; z <= maxDistance; z++) {
                    for (int y = -maxDistance; y <= maxDistance; y++) {
                        if (x * x + z * z + y * y <= maxDistance * maxDistance)
                            suitablePosesAround.add(new Vec3i(x, z, y));
                    }
                }
            }
            System.out.println(suitablePosesAround.size());
        }

        {
            List<Vec2i> suitablePosesAround = new ArrayList<>(4000);
            int maxDistance = 19;
            for (int x = -maxDistance; x <= maxDistance; x++) {
                for (int z = -maxDistance; z <= maxDistance; z++) {
                    if (x * x + z * z <= maxDistance * maxDistance)
                        suitablePosesAround.add(new Vec2i(x, z));
                }
            }
            System.out.println(suitablePosesAround.size());
        }
    }
}
