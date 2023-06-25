package at.haha007.edenclient.utils.tasks;

import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;

public class WaitForInventoryNameTask extends WaitingTask {
    public WaitForInventoryNameTask(Pattern pattern) {
        super(() -> {
            Screen sc = Minecraft.getInstance().screen;
            if (sc == null) return false;
            if(!(sc instanceof ContainerScreen))return false;
            Component title = sc.getTitle();
            if (title == null) return false;
            return pattern.matcher(title.getString()).matches();
        });
    }
}
