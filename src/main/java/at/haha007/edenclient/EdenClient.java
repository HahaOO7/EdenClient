package at.haha007.edenclient;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EdenClient implements ClientModInitializer {
    public static EdenClient INSTANCE;
    private static final Map<Class<?>, Object> registeredMods = new HashMap<>();
    public static ExecutorService chatThread = Executors.newSingleThreadExecutor();

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        PerWorldConfig.get();
        getModsClassesFromFile().forEach(this::registerMod);
    }

    private void registerMod(Class<?> clazz) {
        try {
            LogUtils.getLogger().info("[EC] Registering mod: %s".formatted(clazz.getCanonicalName()));
            Constructor<?> constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            Object object = constructor.newInstance();
            registeredMods.put(object.getClass(), object);
        } catch (InstantiationException |
                 IllegalAccessException |
                 InvocationTargetException |
                 NoSuchMethodException e) {
            LogUtils.getLogger().trace("Error while enabling mod: " + clazz.getCanonicalName(), e);
        }
    }

    public static <T> T getMod(Class<T> clazz) {
        //noinspection unchecked
        return (T) registeredMods.get(clazz);
    }

    public static File getDataFolder() {
        File file = getConfigDirectory();
        if (!file.exists())
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        return file;
    }

    public static File getConfigDirectory() {
        return new File(Minecraft.getInstance().gameDirectory, "config");
    }

    private List<Class<?>> getModsClassesFromFile() {
        record AnnotatedClass(Class<?> clazz, List<Class<?>> dependencies) {
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mods.txt")) {
            if (is == null) throw new IOException("File not found: mods.txt");
            String[] strings = new String(is.readAllBytes()).split("\n");
            List<AnnotatedClass> annotatedClasses = new ArrayList<>();
            for (String string : strings) {
                Class<?> clazz = Class.forName(string);
                Mod mod = clazz.getAnnotation(Mod.class);
                List<Class<?>> dependencies = Arrays.stream(mod.dependencies()).distinct().sorted().toList();
                AnnotatedClass annotatedClass = new AnnotatedClass(clazz, dependencies);
                annotatedClasses.add(annotatedClass);
            }

            List<AnnotatedClass> sortedClasses = new ArrayList<>();
            for (AnnotatedClass annotatedClass : annotatedClasses) {
                List<Class<?>> dependencies = new ArrayList<>(annotatedClass.dependencies());
                for (Class<?> dependency : dependencies) {
                    if (dependency == annotatedClass.clazz())
                        throw new IllegalStateException("Mods cannot depend on themself: %s".formatted(dependency.getCanonicalName()));

                    if (!annotatedClasses.stream().map(AnnotatedClass::clazz).toList().contains(dependency)) {
                        throw new IllegalStateException("Declared dependency is not a Mod: %s -> %s"
                                .formatted(annotatedClass.clazz().getCanonicalName(), dependency.getCanonicalName()));
                    }
                }

                int index = 0;
                for (int i = 0; i < sortedClasses.size(); i++) {
                    if (index >= sortedClasses.size() || dependencies.isEmpty())
                        break;
                    dependencies.remove(sortedClasses.get(index).clazz());
                    index++;
                }
                if (!dependencies.isEmpty())
                    throw new IllegalStateException("Dependency Error: %s"
                            .formatted(String.join(", ", dependencies.stream().map(Class::getCanonicalName).toList())));
                sortedClasses.add(index, annotatedClass);
            }
            return sortedClasses.stream().map(AnnotatedClass::clazz).collect(Collectors.toList());
        } catch (IOException | ClassNotFoundException e) {
            LogUtils.getLogger().trace("Error while loading mods: ", e);
        }
        return new ArrayList<>();
    }
}
