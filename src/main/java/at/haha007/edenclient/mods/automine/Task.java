package at.haha007.edenclient.mods.automine;

import net.minecraft.client.network.ClientPlayerEntity;

interface Task {

	//returns true if it should continue
	//returns false when the task is completed
	boolean tick(ClientPlayerEntity player);

}
