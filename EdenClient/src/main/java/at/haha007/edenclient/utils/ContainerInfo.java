package at.haha007.edenclient.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ContainerInfo {
    private static final Cache<Integer, ContainerInfo> cache = CacheBuilder.newBuilder().maximumSize(100).build();

    public static ContainerInfo get(int id) {
        try {
            return cache.get(id, ContainerInfo::new);
        } catch (ExecutionException e) {
            StringUtils.getLogger().error("Failed to get ContainerInfo for id: " + id, e);
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

    public Component getTitle() {
        return title;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public MenuType<?> getType() {
        return type;
    }
}
