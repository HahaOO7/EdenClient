package at.haha007.edenclient.utils.config;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.utils.StringUtils;
import at.haha007.edenclient.utils.config.loaders.*;
import at.haha007.edenclient.utils.config.wrappers.*;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Style;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PerWorldConfig {

    private static PerWorldConfig INSTANCE;
    private final Map<String, Object> registered = new HashMap<>();
    private final Map<Class<?>, ConfigLoader<NbtElement, ?>> loaders = new HashMap<>();
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

    public void register(ConfigLoader<? extends NbtElement, ?> loader, Class<?> loadableType) {
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

        register(new BlockBoxLoader(), BlockBox.class);

        register(new BlockLoader(), Block.class);
        register(new BlockSetLoader(), BlockSet.class);

        register(new BlockEntityTypeLoader(), BlockEntityType.class);
        register(new BlockEntityTypeSetLoader(), BlockEntityTypeSet.class);

        register(new StyleLoader(), Style.class);

        register(new Vec3iLoader(), Vec3i.class);
        register(new StringVec3iMapLoader(), StringVec3iMap.class);

        register(new BiStringStringMapLoader(), BiStringStringMap.class);

        register(new ChunkPosLoader(), ChunkPos.class);
    }

    @SuppressWarnings("unchecked")
    private ConfigLoader<NbtElement, ?> castLoader(Object object) {
        return (ConfigLoader<NbtElement, ?>) object;
    }

    private void onLeave() {
        long start = System.nanoTime();
        System.out.println("[EC] Start saving config: " + worldName);
        saveConfig();
        //noinspection RedundantStringFormatCall
        System.out.println(String.format("[EC] Saving done, this took %sms.", TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start))));
    }

    private void onJoin() {
        worldName = StringUtils.getWorldOrServerName();
        long start = System.nanoTime();
        System.out.println("[EC] Start loading config: " + worldName);
        loadConfig();
        //noinspection RedundantStringFormatCall
        System.out.println(String.format("[EC] Loading done, this took %sms.", TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start))));
    }

    private void loadConfig() {
        File file = new File(folder, worldName + ".mca");
        if (!folder.exists()) folder.mkdirs();
        NbtCompound tag = new NbtCompound();
        try {
            tag = file.exists() ? NbtIo.readCompressed(file) : new NbtCompound();
        } catch (IOException e) {
            System.err.println("Error while loading PerWorldConfig: " + worldName);
        }
        NbtCompound finalTag = tag;
        registered.forEach((key, obj) -> load(getCompound(finalTag, key), obj));
    }

    private void saveConfig() {
        NbtCompound tag = new NbtCompound();
        registered.forEach((key, obj) -> {
            NbtCompound compound = getCompound(tag, key);
            save(compound, obj);
        });
        File file = new File(folder, worldName + ".mca");
        if (!folder.exists()) folder.mkdirs();
        try {
            NbtIo.writeCompressed(tag, file);
        } catch (IOException e) {
            System.err.println("Error while saving PerWorldConfig: " + worldName);
        }
    }


    private void load(NbtCompound tag, Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            var annotation = field.getDeclaredAnnotation(ConfigSubscriber.class);
            if (annotation == null) continue;
            Class<?> c = getClass(field);
            ConfigLoader<NbtElement, ?> loader = getLoader(c);
            if (loader == null) {
                System.err.println("Error loading config: No loader found for class: " + c.getCanonicalName());
                continue;
            }
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                NbtElement nbt = tag.contains(fieldName) ? tag.get(fieldName) : loader.parse(annotation.value());
                Object value;
                try {
                    value = loader.load(nbt);
                } catch (ClassCastException e) {
                    System.err.println("Error while loading " + field.getName() + " in class " + obj.getClass().getSimpleName());
                    e.printStackTrace();
                    value = loader.load(loader.parse(annotation.value()));
                }
                field.set(obj, value);
            } catch (IllegalAccessException e) {
                System.err.println("Error loading config: Can't access field: " + c.getCanonicalName() + "." + field.getName());
            }
        }
    }

    private void save(NbtCompound tag, Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigSubscriber.class)) continue;
            Class<?> c = getClass(field);
            ConfigLoader<NbtElement, ?> loader = getLoader(c);
            if (loader == null) {
                System.err.println("Error loading config: No loader found for class: " + c.getCanonicalName());
                continue;
            }
            try {
                field.setAccessible(true);
                tag.put(field.getName(), loader.save(field.get(obj)));
            } catch (IllegalAccessException e) {
                System.err.println("Error loading config: Can't access field: " + c.getCanonicalName() + "." + field.getName());
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

    private ConfigLoader<NbtElement, ?> getLoader(Class<?> clazz) {
        if (null == clazz) return null;
        ConfigLoader<NbtElement, ?> loader = loaders.get(getClass(clazz));
        if (loader == null) return getLoader(clazz.getSuperclass());
        return loader;
    }

    private NbtCompound getCompound(NbtCompound root, String path) {
        if (path.isEmpty()) return root;
        String[] a = path.split("\\.");
        for (String s : a) {
            NbtCompound tag = root.getCompound(s);
            root.put(s, tag);
            root = tag;
        }
        return root;
    }

    public NbtElement toNbt(Object object) {
        ConfigLoader<NbtElement, ?> loader = getLoader(object.getClass());
        if (loader == null) {
            System.err.println("Error loading config: No loader found for class: " + object.getClass().getCanonicalName());
            return null;
        }
        return loader.save(object);
    }

    public <T> T toObject(NbtElement nbt, Class<T> type) {
        ConfigLoader<NbtElement, ?> loader = getLoader(type);
        if (loader == null) {
            System.err.println("Error loading config: No loader found for class: " + type.getCanonicalName());
            return null;
        }
        //noinspection unchecked
        return (T) loader.load(nbt);
    }
}
