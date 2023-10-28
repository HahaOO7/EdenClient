package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class StyleLoader implements ConfigLoader<CompoundTag, Style> {
    @Override
    public CompoundTag save(Style s) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("bold", s.isBold());
        tag.putBoolean("obfuscated", s.isObfuscated());
        tag.putBoolean("italic", s.isItalic());
        tag.putBoolean("strikethrough", s.isStrikethrough());
        tag.putBoolean("underlined", s.isUnderlined());
        TextColor rgb = s.getColor();
        if (rgb != null)
            tag.putInt("rgb", rgb.getValue());
        return tag;
    }

    @Override
    public Style load(CompoundTag tag) {
        Style style = Style.EMPTY;
        style = style.withBold(tag.getBoolean("bold"));
        style = style.withObfuscated(tag.getBoolean("obfuscated"));
        style = style.withItalic(tag.getBoolean("italic"));
        style = style.withStrikethrough(tag.getBoolean("strikethrough"));
        style = style.withUnderlined(tag.getBoolean("underlined"));
        if (tag.contains("rgb"))
            style = style.withColor(tag.getInt("rgb"));
        return style;
    }

    @Override
    public CompoundTag parse(String s) {
        return new CompoundTag();
    }
}
