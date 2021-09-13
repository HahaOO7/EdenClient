package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

public class EntityTypeLoader implements ConfigLoader<NbtString, EntityType<?>> {

    private static final DefaultedRegistry<EntityType<?>> registry = Registry.ENTITY_TYPE;

    public NbtString save(Object value) {
        return NbtString.of(registry.getId(cast(value)).toString());
    }

    public EntityType<?> load(NbtString nbtElement) {
        return registry.get(new Identifier(nbtElement.asString()));
    }

    public NbtString parse(String s) {
        return NbtString.of("minecraft:" + s);
    }
}
