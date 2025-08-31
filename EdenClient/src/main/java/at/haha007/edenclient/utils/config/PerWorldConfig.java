package at.haha007.edenclient.utils.config;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.area.*;
import at.haha007.edenclient.utils.config.loaders.*;
import at.haha007.edenclient.utils.config.wrappers.*;
import com.mojang.logging.LogUtils;
import fi.dy.masa.malilib.util.nbt.PrettyNbtStringifier;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PerWorldConfig {

    private static final PerWorldConfig INSTANCE = new PerWorldConfig();
    private final Map<String, Object> registered = new HashMap<>();
    private final Map<Class<?>, ConfigLoader<Tag, ?>> loaders = new HashMap<>();
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
        JoinWorldCallback.EVENT.register(this::onJoin, getClass());
        LeaveWorldCallback.EVENT.register(this::onLeave, getClass());
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
        LogUtils.getLogger().info("Start saving config: {}", worldName);
        saveConfig();
        LogUtils.getLogger().info("Saving done, this took {}ms.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    private void onJoin() {
        onLeave();
        long start = System.nanoTime();
        EdenClient.getMod(Scheduler.class).runAsync(() -> {
            try {
                Thread.sleep(1000);
                worldName = getWorldOrServerName();
                if (worldName == null || worldName.equals("null")) {
                    LogUtils.getLogger().error("World is null!", new NullPointerException());
                }
                LogUtils.getLogger().info("Start loading config: {}", worldName);
                loadConfig();
                LogUtils.getLogger().info("Loading done, this took %sms.%n"
                        .formatted(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
                ConfigLoadedCallback.EVENT.invoker().configLoaded();
            } catch (InterruptedException e) {
                LogUtils.getLogger().error("Error while loading PerWorldConfig: {}", worldName, e);
                Thread.currentThread().interrupt();
            }
        });
    }

    private void loadConfig() {
        File file = new File(folder, worldName + ".mca");
        if (ensureExistingConfigFolder()) return;
        CompoundTag tag = new CompoundTag();
        try {
            tag = file.exists() ? NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap()) : new CompoundTag();
        } catch (IOException e) {
            LogUtils.getLogger().error("Error while loading PerWorldConfig: {}", worldName, e);
        }
        LogUtils.getLogger().info("LOADING CONFIG: {}", worldName);
        @SuppressWarnings("UnstableApiUsage")
        String json = String.join("\n", new PrettyNbtStringifier().getNbtLines(tag));
//        LogUtils.getLogger().info("LOADED VALUES: {}", json);
        LogUtils.getLogger().info("FROM: {}", file);
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
        LogUtils.getLogger().info("TO: {}", file);
        if (ensureExistingConfigFolder()) return;
        try {
            NbtIo.writeCompressed(tag, file.toPath());
            worldName = null;
        } catch (IOException e) {
            LogUtils.getLogger().error("Error while saving PerWorldConfig: {}", worldName, e);
        }
    }

    private boolean ensureExistingConfigFolder() {
        if (folder.exists() || folder.mkdirs()) {
            return false;
        }
        LogUtils.getLogger().error("Failed to create config folder!");
        return true;
    }


    private void load(CompoundTag tag, Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            var annotation = field.getDeclaredAnnotation(ConfigSubscriber.class);
            if (annotation == null) continue;
            Class<?> c = getClass(field);
            if (c == null) {
                LogUtils.getLogger().error("Error loading config while loading: No class found for field: {}.{}",
                        obj.getClass().getSimpleName(), field.getName());
                continue;
            }
            ConfigLoader<Tag, ?> loader = getLoader(c);
            if (loader == null) {
                LogUtils.getLogger().error("Error loading config: No loader found for class: {}", c.getCanonicalName());
                continue;
            }
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                Tag nbt = tag.contains(fieldName) ? tag.get(fieldName) : loader.parse(annotation.value());
                if (nbt == null) {
                    LogUtils.getLogger().error("Error loading config: Field is null: {} in class {} of type {}",
                            fieldName,
                            field.getDeclaringClass().getSimpleName(),
                            loader.getClass().getSimpleName());
                    continue;
                }
                Object value;
                try {
                    value = loader.load(nbt);
                } catch (ClassCastException | IllegalArgumentException e) {
                    LogUtils.getLogger().error("Error while loading {} in class {}", field.getName(), obj.getClass().getSimpleName(), e);
                    value = loader.load(loader.parse(annotation.value()));
                }
                field.set(obj, value);
            } catch (IllegalAccessException e) {
                LogUtils.getLogger().error("Error loading config: Can't access field: {}.{}", c.getCanonicalName(), field.getName(), e);
            }
        }
    }

    private void save(CompoundTag tag, Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigSubscriber.class)) continue;
            Class<?> c = getClass(field);
            if (c == null) {
                LogUtils.getLogger().error("Error loading config while saving: No class found for field: {}.{}",
                        obj.getClass().getSimpleName(), field.getName());
                continue;
            }
            @SuppressWarnings("unchecked")
            ConfigLoader<Tag, Object> loader = (ConfigLoader<Tag, Object>) getLoader(c);
            if (loader == null) {
                LogUtils.getLogger().error("Error loading config: No loader found for class: {}", c.getCanonicalName(), new NullPointerException());
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) continue;
                Tag nbt = loader.save(value);
                tag.put(field.getName(), nbt);
            } catch (IllegalAccessException e) {
                LogUtils.getLogger().error("Error loading config: Can't access field: {}.{}", c.getCanonicalName(), field.getName(), e);
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
            CompoundTag tag = root.getCompound(s).orElse(new CompoundTag());
            root.put(s, tag);
            root = tag;
        }
        return root;
    }

    public Tag toNbt(Object object) {
        //noinspection unchecked
        ConfigLoader<Tag, Object> loader = (ConfigLoader<Tag, Object>) getLoader(object.getClass());
        if (loader == null) {
            LogUtils.getLogger().error("Error loading config: No loader found for class: {}", object.getClass().getCanonicalName(), new NullPointerException());
            return null;
        }
        return loader.save(object);
    }

    public <T> T toObject(@NotNull Tag nbt, @NotNull Class<T> type) {
        ConfigLoader<Tag, ?> loader = getLoader(type);
        if (loader == null) {
            LogUtils.getLogger().error("Error loading config: No loader found for class: {}", type.getCanonicalName(), new NullPointerException());
            return null;
        }
        //noinspection unchecked
        return (T) loader.load(nbt);
    }


    private String getWorldOrServerName() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        if (mc.hasSingleplayerServer()) {
            net.minecraft.client.server.IntegratedServer server = mc.getSingleplayerServer();

            if (server != null) {
                return server.getWorldData().getLevelName();
            }
        }

        net.minecraft.client.multiplayer.ServerData server = mc.getCurrentServer();
        if (server != null) {
            return server.ip.trim().replace(':', '_');
        }

        net.minecraft.client.multiplayer.ClientPacketListener handler = mc.getConnection();
        net.minecraft.network.Connection connection = handler != null ? handler.getConnection() : null;
        if (connection != null) {
            return "realms_" + stringifyAddress(connection.getRemoteAddress());
        }

        return null;
    }

    private String stringifyAddress(SocketAddress address) {
        String str = address.toString();

        if (str.contains("/")) {
            str = str.substring(str.indexOf('/') + 1);
        }

        return str.replace(':', '_');
    }
}
