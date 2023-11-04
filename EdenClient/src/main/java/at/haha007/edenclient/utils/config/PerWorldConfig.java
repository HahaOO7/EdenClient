package at.haha007.edenclient.utils.config;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.Utils;
import at.haha007.edenclient.utils.area.*;
import at.haha007.edenclient.utils.config.loaders.*;
import at.haha007.edenclient.utils.config.wrappers.*;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PerWorldConfig {

    private static PerWorldConfig INSTANCE;
    private final Map<String, Object> registered = new HashMap<>();
    private final Map<Class<?>, ConfigLoader<Tag, ?>> loaders = new HashMap<>();
    //    private NbtCompound tag = new NbtCompound();
    private String worldName = "null";
    private final File folder;
    private final Map<Class<?>, Class<?>> wrapperClasses = Map.of(
            double.class, Double.class,
            float.class, Float.class,
            int.class, Integer.class,
            short.class, Short.class,
            byte.class, Byte.class,
            boolean.class, Boolean.class
    );

    public static PerWorldConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new PerWorldConfig();
        }
        return INSTANCE;
    }

    public void register(Object obj, String path) {
        registered.put(path, obj);
    }

    public void register(ConfigLoader<? extends Tag, ?> loader, Class<?> loadableType) {
        loaders.put(loadableType, castLoader(loader));
    }

    private PerWorldConfig() {
        folder = new File(EdenClient.getDataFolder(), "PerWorldCfg");
        JoinWorldCallback.EVENT.register(this::onJoin);
        LeaveWorldCallback.EVENT.register(this::onLeave);
        register(new IntegerLoader(), Integer.class);
        register(new BooleanLoader(), Boolean.class);

        register(new FloatLoader(), Float.class);
        register(new DoubleLoader(), Double.class);

        register(new ItemLoader(), Item.class);
        register(new ItemSetLoader(), ItemSet.class);
        register(new ItemListLoader(), ItemList.class);

        register(new EntityTypeLoader(), EntityType.class);
        register(new EntityTypeSetLoader(), EntityTypeSet.class);

        register(new StringLoader(), String.class);
        register(new StringListLoader(), StringList.class);
        register(new StringSetLoader(), StringSet.class);
        register(new StringStringMapLoader(), StringStringMap.class);

        register(new BlockBoxLoader(), BoundingBox.class);

        register(new BlockLoader(), Block.class);
        register(new BlockSetLoader(), BlockSet.class);

        register(new BlockEntityTypeLoader(), BlockEntityType.class);
        register(new BlockEntityTypeSetLoader(), BlockEntityTypeSet.class);

        register(new StyleLoader(), Style.class);

        register(new Vec3iLoader(), Vec3i.class);
        register(new StringVec3iMapLoader(), StringVec3iMap.class);

        register(new BiStringStringMapLoader(), BiStringStringMap.class);

        register(new ChunkPosLoader(), ChunkPos.class);

        register(new SavableBlockAreaLoader(), SavableBlockArea.class);
        register(new CubeAreaLoader(), CubeArea.class);
        register(new SphereAreaLoader(), SphereArea.class);
        register(new CylinderAreaLoader(), CylinderArea.class);

    }

    @SuppressWarnings("unchecked")
    private ConfigLoader<Tag, ?> castLoader(Object object) {
        return (ConfigLoader<Tag, ?>) object;
    }

    private void onLeave() {
        if (worldName == null || worldName.equals("null")) return;
        long start = System.nanoTime();
        Utils.getLogger().info("Start saving config: " + worldName);
        saveConfig();
        Utils.getLogger().info(String.format("Saving done, this took %sms.", TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start))));
    }

    private void onJoin() {
        onLeave();
        long start = System.nanoTime();
        EdenClient.getMod(Scheduler.class).runAsync(() -> {
            try {
                Thread.sleep(1000);
                worldName = Utils.getWorldOrServerName();
                if (worldName == null || worldName.equals("null")) {
                    Utils.getLogger().error("World is null!", new NullPointerException());
                }
                Utils.getLogger().info("Start loading config: " + worldName);
                loadConfig();
                Utils.getLogger().info("Loading done, this took %sms.%n"
                        .formatted(TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start))));
                ConfigLoadedCallback.EVENT.invoker().configLoaded();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void loadConfig() {
        File file = new File(folder, worldName + ".mca");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Utils.getLogger().error("Failed to create config folder!");
                return;
            }
        }
        CompoundTag tag = new CompoundTag();
        try {
            tag = file.exists() ? NbtIo.readCompressed(file) : new CompoundTag();
        } catch (IOException e) {
            Utils.getLogger().error("Error while loading PerWorldConfig: " + worldName, e);
        }
        CompoundTag finalTag = tag;
        registered.forEach((key, obj) -> load(getCompound(finalTag, key), obj));
    }

    private void saveConfig() {
        CompoundTag tag = new CompoundTag();
        registered.forEach((key, obj) -> {
            CompoundTag compound = getCompound(tag, key);
            save(compound, obj);
        });
        File file = new File(folder, worldName + ".mca");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Utils.getLogger().error("Failed to create config folder!");
                return;
            }
        }
        try {
            NbtIo.writeCompressed(tag, file);
        } catch (IOException e) {
            Utils.getLogger().error("Error while saving PerWorldConfig: " + worldName, e);
        }
    }


    private void load(CompoundTag tag, Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            var annotation = field.getDeclaredAnnotation(ConfigSubscriber.class);
            if (annotation == null) continue;
            Class<?> c = getClass(field);
            ConfigLoader<Tag, ?> loader = getLoader(c);
            if (loader == null) {
                Utils.getLogger().error("Error loading config: No loader found for class: " + c.getCanonicalName());
                continue;
            }
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                Tag nbt = tag.contains(fieldName) ? tag.get(fieldName) : loader.parse(annotation.value());
                Object value;
                try {
                    value = loader.load(nbt);
                } catch (ClassCastException | IllegalArgumentException e) {
                    Utils.getLogger().error("Error while loading " + field.getName() + " in class " + obj.getClass().getSimpleName(), e);
                    value = loader.load(loader.parse(annotation.value()));
                }
                field.set(obj, value);
            } catch (IllegalAccessException e) {
                Utils.getLogger().error("Error loading config: Can't access field: " + c.getCanonicalName() + "." + field.getName(), e);
            }
        }
    }

    private void save(CompoundTag tag, Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigSubscriber.class)) continue;
            Class<?> c = getClass(field);
            @SuppressWarnings("unchecked")
            ConfigLoader<Tag, Object> loader = (ConfigLoader<Tag, Object>) getLoader(c);
            if (loader == null) {
                Utils.getLogger().error("Error loading config: No loader found for class: " + c.getCanonicalName(), new NullPointerException());
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) continue;
                Tag nbt = loader.save(value);
                if (nbt == null) continue;
                tag.put(field.getName(), nbt);
            } catch (IllegalAccessException e) {
                Utils.getLogger().error("Error loading config: Can't access field: " + c.getCanonicalName() + "." + field.getName(), e);
            }
        }
    }

    private Class<?> getClass(Field field) {
        Class<?> clazz = field.getType();
        return wrapperClasses.getOrDefault(clazz, clazz);
    }

    private Class<?> getClass(Class<?> clazz) {
        return wrapperClasses.getOrDefault(clazz, clazz);
    }

    private <T> ConfigLoader<Tag, ? super T> getLoader(Class<T> clazz) {
        if (null == clazz) return null;
        ConfigLoader<Tag, ?> loader = loaders.get(getClass(clazz));
        if (loader == null) return getLoader(clazz.getSuperclass());
        //noinspection unchecked
        return (ConfigLoader<Tag, ? super T>) loader;
    }

    private CompoundTag getCompound(CompoundTag root, String path) {
        if (path.isEmpty()) return root;
        String[] a = path.split("\\.");
        for (String s : a) {
            CompoundTag tag = root.getCompound(s);
            root.put(s, tag);
            root = tag;
        }
        return root;
    }

    public Tag toNbt(Object object) {
        //noinspection unchecked
        ConfigLoader<Tag, Object> loader = (ConfigLoader<Tag, Object>) getLoader(object.getClass());
        if (loader == null) {
            Utils.getLogger().error("Error loading config: No loader found for class: " + object.getClass().getCanonicalName(), new NullPointerException());
            return null;
        }
        return loader.save(object);
    }

    public <T> T toObject(Tag nbt, Class<T> type) {
        ConfigLoader<Tag, ?> loader = getLoader(type);
        if (loader == null) {
            Utils.getLogger().error("Error loading config: No loader found for class: " + type.getCanonicalName(), new NullPointerException());
            return null;
        }
        //noinspection unchecked
        return (T) loader.load(nbt);
    }
}
