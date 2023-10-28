package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class EntityTypeLoader implements ConfigLoader<StringTag, EntityType<?>> {

    private static final DefaultedRegistry<EntityType<?>> registry = BuiltInRegistries.ENTITY_TYPE;

    public StringTag save(EntityType<?> value) {
        return StringTag.valueOf(registry.getKey(value).toString());
    }

    public EntityType<?> load(StringTag nbtElement) {
        return registry.get(new ResourceLocation(nbtElement.getAsString()));
    }

    public StringTag parse(String s) {
        return StringTag.valueOf("minecraft:" + s);
    }
}
