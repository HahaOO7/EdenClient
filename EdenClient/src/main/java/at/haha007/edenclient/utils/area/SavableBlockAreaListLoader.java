package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class SavableBlockAreaListLoader implements ConfigLoader<ListTag, SavableBlockAreaList> {
    private static final SavableBlockAreaLoader areaLoader = new SavableBlockAreaLoader();

    @Override
    public @NonNull ListTag save(@NonNull SavableBlockAreaList value) {
        ListTag listTag = new ListTag();
        for (SavableBlockArea area : value) {
            listTag.add(areaLoader.save(area));
        }
        return listTag;
    }

    @Override
    public @NonNull SavableBlockAreaList load(@NonNull ListTag nbtElement) {
        LogUtils.getLogger().warn("Loading SavableBlockAreaList with {} elements", nbtElement.size());
        SavableBlockAreaList areaList = new SavableBlockAreaList();
        for (int i = 0; i < nbtElement.size(); i++) {
            Optional<CompoundTag> compoundTag = nbtElement.getCompound(i);
            compoundTag.ifPresent(tag -> areaList.add(areaLoader.load(tag)));
        }
        LogUtils.getLogger().warn("Loaded SavableBlockAreaList with {} elements", areaList.size());
        return areaList;
    }

    @Override
    public @NonNull ListTag parse(@NotNull String s) {
        return new ListTag();
    }
}
