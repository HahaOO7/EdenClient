package at.haha007.edenclient.utils;

import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Getter
public class ContainerInfo {
    private static final Cache<Integer, ContainerInfo> cache = CacheBuilder.newBuilder().maximumSize(100).build();

    static{
        LeaveWorldCallback.EVENT.register(cache::invalidateAll, ContainerInfo.class);
    }

    public static ContainerInfo get(int id) {
        try {
            return cache.get(id, ContainerInfo::new);
        } catch (ExecutionException e) {
            LogUtils.getLogger().error("Failed to get ContainerInfo for id: {}", id, e);
            return new ContainerInfo();
        }
    }

    public static ContainerInfo update(int id, List<ItemStack> items) {
        ContainerInfo containerInfo = get(id);
        containerInfo.update(items);
        return containerInfo;
    }

    public static ContainerInfo update(int id, MenuType<?> type, Component title) {
        ContainerInfo containerInfo = get(id);
        containerInfo.update(type, title);
        return containerInfo;
    }

    public static void remove(int id) {
        cache.invalidate(id);
    }

    private MenuType<?> type = null;
    private Component title = null;
    private List<ItemStack> items = null;

    public boolean isComplete() {
        return type != null && title != null && items != null;
    }

    private void update(List<ItemStack> items) {
        this.items = items;
    }

    private void update(MenuType<?> type, Component title) {
        this.type = type;
        this.title = title;
    }

}
