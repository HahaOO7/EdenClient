package at.haha007.edenclient.interfaces;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.gen.Accessor;

public interface TextFieldWidgetInterface {
    int getMaxLength();
}
