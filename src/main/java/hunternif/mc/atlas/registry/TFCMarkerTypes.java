package hunternif.mc.atlas.registry;

import su.tfcsurvival.api.types.Ore;
import net.minecraft.util.ResourceLocation;

public class TFCMarkerTypes {
    public static void init() {
        for (Ore o : Ore.values()) {
            registerGradedMarkerIcon(o, gradePrefix(Ore.Grade.NORMAL));

            if (o.isGraded()) {
                registerGradedMarkerIcon(o, gradePrefix(Ore.Grade.POOR));
                registerGradedMarkerIcon(o, gradePrefix(Ore.Grade.RICH));
            }
        }
    }

    private static void registerGradedMarkerIcon(Ore o, String grade) {
        ResourceLocation textureLoc = new ResourceLocation("tfc", "textures/items/ore/" + grade + o.name() + ".png");

        MarkerType type = new MarkerType(new ResourceLocation(getRegistryName(o, grade)), textureLoc) {
            @Override
            public boolean isVisibleInList() {
                return false;
            }
        };
        type.setSize(1);
        System.out.println("Registering tfc marker " + type.getRegistryName());
        MarkerRegistry.register(type);
    }

    public static String getRegistryName(Ore o, String grade) {
        return "aa_item:" + grade + o.name();
    }

    public static String gradePrefix(Ore.Grade value) {
        switch (value) {
            case NORMAL:
                return "";
            case POOR:
                return "poor/";
            case RICH:
                return "rich/";
            default:
                throw new IllegalArgumentException("unsupported ore grade");
        }
    }
}
