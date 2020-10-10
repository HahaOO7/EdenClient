package at.haha007.edenclient.mods.automine;

import net.minecraft.client.network.ClientPlayerEntity;

public class TaskWait implements Task {
	private int timeout;

	TaskWait(int ticks) {
		timeout = ticks;
	}

	public boolean tick(ClientPlayerEntity player) {
		return --timeout >= 0;
	}
}
