package at.haha007.edenclient.utils.config;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.utils.StringUtils;
import at.haha007.edenclient.utils.config.loaders.BooleanLoader;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.loaders.IntegerLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;

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
    private NbtCompound tag = new NbtCompound();
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

    public void register(ConfigLoader<NbtElement, ?> loader, Class<?> loadableType) {
        loaders.put(loadableType, loader);
    }

    private PerWorldConfig() {
        folder = new File(EdenClient.getDataFolder(), "PerWorldCfg");
        JoinWorldCallback.EVENT.register(this::onJoin);
        LeaveWorldCallback.EVENT.register(this::onLeave);
        register(castLoader(new IntegerLoader()), Integer.class);
        register(castLoader(new BooleanLoader()), Boolean.class);
    }

    @SuppressWarnings("unchecked")
    private ConfigLoader<NbtElement, ?> castLoader(Object object) {
        return (ConfigLoader<NbtElement, ?>) object;
    }

    @SuppressWarnings("RedundantStringFormatCall")
    private void onLeave() {
        long start = System.nanoTime();
        System.out.println("[EC] Start saving config: " + worldName);
        saveConfig();
        System.out.println(String.format("[EC] Saving done, this took %sms.%n%n", TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start))));
    }

    @SuppressWarnings("RedundantStringFormatCall")
    private void onJoin() {
        worldName = StringUtils.getWorldOrServerName();
        long start = System.nanoTime();
        System.out.println("[EC] Start loading config: " + worldName);
        loadConfig();
        System.out.println(String.format("[EC] Loading done, this took %sms.%n", TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start))));
    }

    private void loadConfig() {
        File file = new File(folder, worldName + ".mca");
        if (!folder.exists()) folder.mkdirs();
        try {
            tag = file.exists() ? NbtIo.readCompressed(file) : new NbtCompound();
        } catch (IOException e) {
            System.err.println("Error while loading PerWorldConfig: " + worldName);
            tag = new NbtCompound();
        }
        registered.forEach((key, obj) -> load(tag.getCompound(key), obj));
        ConfigLoadCallback.EVENT.invoker().onLoad(tag);
    }

    private void saveConfig() {
        ConfigSaveCallback.EVENT.invoker().onSave(tag);
        registered.forEach((key, obj) -> save((NbtCompound) tag.put(key, tag.getCompound(key)), obj));
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
            ConfigLoader<NbtElement, ?> loader = loaders.get(c);
            if (loader == null) {
                System.err.println("Error loading config: No loader found for class: " + c.getCanonicalName());
                continue;
            }
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                NbtElement nbt = tag.contains(fieldName) ? tag.get(fieldName) : loader.parse(annotation.value());
                Object value = loader.load(nbt);
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
            ConfigLoader<NbtElement, ?> loader = loaders.get(c);
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

}
