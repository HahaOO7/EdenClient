package at.haha007.edenclient.automine;

import net.minecraft.client.network.ClientPlayerEntity;

interface Task {
	boolean tick(ClientPlayerEntity player);
}
