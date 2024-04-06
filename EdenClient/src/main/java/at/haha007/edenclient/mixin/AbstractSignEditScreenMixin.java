package at.haha007.edenclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {
    @Shadow protected abstract void onDone();

    @Shadow @Nullable private TextFieldHelper signField;

    @Shadow @Final private String[] messages;

    @Shadow private int line;

    @Shadow protected abstract void setMessage(String string);

    protected AbstractSignEditScreenMixin(Component component) {
        super(component);
    }

    @Inject(at = @At("HEAD"), method = "init", cancellable = true)
    private void onKeyPressed( CallbackInfo ci) {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).bounds(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build());
        this.signField = new TextFieldHelper(() -> this.messages[this.line],
                this::setMessage, TextFieldHelper.createClipboardGetter(Minecraft.getInstance()),
                TextFieldHelper.createClipboardSetter(Minecraft.getInstance()), string -> true);
        ci.cancel();
    }
}
