package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class EntityTypeLoader implements ConfigLoader<StringTag, EntityType<?>> {

    private static final DefaultedRegistry<EntityType<?>> registry = BuiltInRegistries.ENTITY_TYPE;

    @NotNull
    public StringTag save(@NotNull EntityType<?> value) {
        return StringTag.valueOf(registry.getKey(value).toString());
    }

    @NotNull
    public EntityType<?> load(@NotNull StringTag nbtElement) {
        return registry.get(ResourceLocation.parse(nbtElement.getAsString()));
    }

    @NotNull
    public StringTag parse(@NotNull String s) {
        return StringTag.valueOf("minecraft:" + s);
    }
}
