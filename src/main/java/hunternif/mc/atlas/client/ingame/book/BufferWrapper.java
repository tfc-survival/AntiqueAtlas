package hunternif.mc.atlas.client.ingame.book;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;

public class BufferWrapper {
    private final BufferBuilder buffer;
    private final double scale;

    public BufferWrapper(BufferBuilder buffer, double scale) {
        this.buffer = buffer;
        this.scale = scale;
    }
    public BufferBuilder pos(double x, double y, double z) {
        return buffer.pos(x * scale, y * scale, z * scale);
    }

    public void  begin(int glMode, VertexFormat format){
        buffer.begin(glMode, format);
    }
}
