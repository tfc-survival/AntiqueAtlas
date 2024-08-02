package hunternif.mc.atlas.map.objects.path;

import com.google.common.collect.ImmutableSet;
import hunternif.mc.atlas.SettingsConfig;
import hunternif.mc.atlas.map.objects.marker.MarkersData;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.bidirectional.DeletePathPacket;
import hunternif.mc.atlas.util.IntVec2;
import hunternif.mc.atlas.util.Log;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DimensionPathsData {

    private final PathsData parent;

    private Map<Integer, Path> idToPath = new HashMap<>();

    private Map<IntVec2, Set<Path>> chunkToPath = new HashMap<>();

    public DimensionPathsData(PathsData parent) {
        this.parent = parent;
    }

    public Set<Path> getPathsInChunk(int x, int z) {
        return chunkToPath.getOrDefault(new IntVec2(x, z), ImmutableSet.of());
    }

    public void loadPath(Path path) {
        if (!idToPath.containsKey(path.id)) {
            if (idToPath.size() < SettingsConfig.performance.pathLimit) {
                idToPath.put(path.id, path);
                if (chunkIndexation(path)) {
                    PacketDispatcher.sendToServer(new DeletePathPacket(parent.atlasId, path.id));
                    removePath(path.id);
                }
            } else {
                Log.warn("Could not add new path. Atlas is at it's limit of %d paths", SettingsConfig.performance.pathLimit);
            }
        }
    }

    public Path createAndSavePath(String label, int color, int startX, int startZ, short[] segments) {
        Path path = new Path(getNewID(), label, color, startX, startZ, segments);
        Log.info("Created new path %s", path.toString());
        idToPath.put(path.id, path);
        parent.markDirty();
        return path;
    }

    public void removePath(int pathID) {
        Path removed = idToPath.remove(pathID);
        parent.markDirty();
        if (removed != null) {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                chunkToPath.values().forEach(e -> e.remove(removed));
            }
        }
    }

    private final AtomicInteger largestID = new AtomicInteger(0);

    private int getNewID() {
        return largestID.incrementAndGet();
    }

    public void readFromNBT(NBTTagCompound nbt) {
        int segmentCount = 0;
        NBTTagList pathesNbt = nbt.getTagList("pathes", Constants.NBT.TAG_COMPOUND);

        if (pathesNbt.tagCount() > SettingsConfig.performance.pathLimit)
            Log.warn("Could not load %d paths. Atlas is at it's limit of %d paths", pathesNbt.tagCount() - SettingsConfig.performance.pathLimit, SettingsConfig.performance.pathLimit);

        for (int i = 0; i < Math.min(pathesNbt.tagCount(), SettingsConfig.performance.pathLimit); i++) {
            Path path = new Path(pathesNbt.getCompoundTagAt(i));

            segmentCount += path.segments.length;
            if (segmentCount > SettingsConfig.performance.pathSegmentLimit) {
                Log.warn("Could not add new path. Atlas is at it's limit of %d path segments", SettingsConfig.performance.pathSegmentLimit);
                break;
            }

            idToPath.put(path.id, path);

            if (largestID.intValue() < path.id) {
                largestID.set(path.id);
            }
        }

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            List<Path> toRemove = new ArrayList<>();
            for (Path path : idToPath.values()) {
                if (chunkIndexation(path))
                    toRemove.add(path);
            }
            toRemove.forEach(p -> {
                PacketDispatcher.sendToServer(new DeletePathPacket(parent.atlasId, p.id));
                removePath(p.id);
            });
        }
    }

    private boolean chunkIndexation(Path path) {
        int x = path.startX;
        int z = path.startZ;
        for (short index : path.segments) {
            Vec3i v = Segment.getVector(index);
            if (v == null) {
                System.out.println("violated path nbt in altas. removing path " + path.id + ", " + Arrays.toString(path.segments));
                return true;
            }
            x += v.getX();
            z += v.getZ();
            IntVec2 key = new IntVec2((x >> 4) / MarkersData.CHUNK_STEP, (z >> 4) / MarkersData.CHUNK_STEP);
            chunkToPath.computeIfAbsent(key, __ -> new HashSet<>()).add(path);
        }
        return false;
    }

    public NBTBase toNbt() {
        NBTTagList pathesNbt = new NBTTagList();
        for (Path path : idToPath.values()) {
            pathesNbt.appendTag(path.toNbt());
        }
        NBTTagCompound r = new NBTTagCompound();
        r.setTag("pathes", pathesNbt);
        return r;
    }

    public boolean isEmpty() {
        return idToPath.isEmpty();
    }

    public Collection<Path> getAllPaths() {
        return idToPath.values();
    }
}
