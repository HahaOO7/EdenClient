package at.haha007.edenclient.utils.screen;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Accessors(fluent = true)
@Getter
@Setter
public class ShowTextScreen extends Screen {
    private Component text;
    private Screen parent;

    public ShowTextScreen(Component text) {
        super(Component.literal("Unused!").withStyle(ChatFormatting.AQUA));
        this.text = text;
        this.parent = Minecraft.getInstance().gui.screen();
    }

    @Override
    protected void init() {
        Button closeButton = Button.builder(Component.literal("Exit").withStyle(ChatFormatting.RED),
                        _ -> Minecraft.getInstance().gui.setScreen(parent))
                .bounds(width - 210, height - 30, 200, 20)
                .tooltip(Tooltip.create(Component.literal("Tooltip of button1")))
                .build();

        SimpleTextWidget textWidget = SimpleTextWidget.builder(text, font)
                .bounds(10, 10, width - 20, height - 20)
                .build();

        addRenderableWidget(closeButton);
        addRenderableWidget(textWidget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, width, height, 0x88000000); //ARGB
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
