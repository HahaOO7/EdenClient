package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class EntityTypeLoader implements ConfigLoader<NbtString, EntityType<?>> {

    private static final DefaultedRegistry<EntityType<?>> registry = Registries.ENTITY_TYPE;

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
