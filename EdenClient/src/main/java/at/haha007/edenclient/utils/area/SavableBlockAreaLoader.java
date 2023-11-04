package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import net.minecraft.nbt.CompoundTag;

public class SavableBlockAreaLoader implements ConfigLoader<CompoundTag, SavableBlockArea> {
    private static final String CUBE_KEY = "cube";
    private static final String CYLINDER_KEY = "cylinder";
    private static final String SPHERE_KEY = "sphere";

    @Override
    public CompoundTag save(SavableBlockArea value) {
        CompoundTag tag = new CompoundTag();
        String type = switch (value.getType()) {
            case CUBE -> CUBE_KEY;
            case CYLINDER -> CYLINDER_KEY;
            case SPHERE -> SPHERE_KEY;
        };
        tag.putString("type", type);
        ConfigLoader<?, BlockArea> loader = switch (value.getType()) {
            case CUBE -> new CubeAreaLoader().asBlockAreaLoader();
            case CYLINDER -> new CylinderAreaLoader().asBlockAreaLoader();
            case SPHERE -> new SphereAreaLoader().asBlockAreaLoader();
        };
        tag.put("value", loader.save(value.getArea()));
        return tag;
    }

    @Override
    public SavableBlockArea load(CompoundTag nbtElement) {
        if(nbtElement == null) return null;
        String type = nbtElement.getString("type");
        ConfigLoader<CompoundTag, BlockArea> loader = switch (type) {
            case CUBE_KEY -> new CubeAreaLoader().asBlockAreaLoader();
            case CYLINDER_KEY -> new CylinderAreaLoader().asBlockAreaLoader();
            case SPHERE_KEY -> new SphereAreaLoader().asBlockAreaLoader();
            default -> null;
        };
        if(loader == null) return null;
        return new SavableBlockArea(loader.load(nbtElement.getCompound("value")));
    }

    @Override
    public CompoundTag parse(String s) {
        return new CubeAreaLoader().parse(s);
    }
}
