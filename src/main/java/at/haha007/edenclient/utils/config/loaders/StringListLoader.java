package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.wrappers.StringList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class StringListLoader implements ConfigLoader<NbtList, StringList> {

    public NbtList save(Object value) {
        NbtList list = new NbtList();
        for (String s : cast(value)) {
            list.add(NbtString.of(s));
        }
        return list;
    }

    public StringList load(NbtList nbtElement) {
        StringList list = new StringList();
        for (NbtElement element : nbtElement) {
            list.add(element.asString());
        }
        return list;
    }

    public NbtList parse(String s) {
        NbtList list = new NbtList();
        if (s.isEmpty()) return list;
        for (String v : s.split(";")) {
            list.add(NbtString.of(v));
        }
        return list;
    }
}
