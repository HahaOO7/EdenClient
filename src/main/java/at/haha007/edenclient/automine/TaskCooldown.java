package at.haha007.edenclient.automine;

import net.minecraft.client.network.ClientPlayerEntity;

class TaskCooldown implements Task {
	private int counter = 0;
	private final int resetTicks;

	TaskCooldown(int ticks) {
		resetTicks = ticks;
	}

	public boolean tick(ClientPlayerEntity player) {
		return (counter = (counter + 1) % resetTicks) != 0;
	}
}
