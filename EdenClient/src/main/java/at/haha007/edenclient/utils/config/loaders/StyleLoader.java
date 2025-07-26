package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

public class StyleLoader implements ConfigLoader<CompoundTag, Style> {
    @Override
    @NotNull
    public CompoundTag save(@NotNull Style s) {
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
    @NotNull
    public Style load(@NotNull CompoundTag tag) {
        Style style = Style.EMPTY;
        style = style.withBold(tag.getBoolean("bold").orElseThrow());
        style = style.withObfuscated(tag.getBoolean("obfuscated").orElseThrow());
        style = style.withItalic(tag.getBoolean("italic").orElseThrow());
        style = style.withStrikethrough(tag.getBoolean("strikethrough").orElseThrow());
        style = style.withUnderlined(tag.getBoolean("underlined").orElseThrow());
        if (tag.contains("rgb"))
            style = style.withColor(tag.getInt("rgb").orElseThrow());
        return style;
    }

    @Override
    @NotNull
    public CompoundTag parse(@NotNull String s) {
        return new CompoundTag();
    }
}
