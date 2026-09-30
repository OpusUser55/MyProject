package dev.ooga.client.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Pressing game keys on the player's behalf, and handing them back cleanly afterwards. */
public final class Keys {
	private Keys() {
	}

	/** Whether the key bound to this mapping is physically held right now. */
	public static boolean physicallyDown(KeyMapping mapping) {
		Minecraft mc = Minecraft.getInstance();
		InputConstants.Key key = ((KeyMappingAccessor) mapping).ooga$getKey();
		if (key.equals(InputConstants.UNKNOWN)) return false;
		long window = mc.getWindow().handle();
		if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
		return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
	}

	public static void hold(KeyMapping mapping) {
		mapping.setDown(true);
	}

	/** One press, as if the key was tapped: queues a click the game handles on its next tick. */
	public static void click(KeyMapping mapping) {
		KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;
		accessor.ooga$setClickCount(accessor.ooga$getClickCount() + 1);
	}

	/** Stops holding the key, unless the player is holding it themselves. */
	public static void release(KeyMapping mapping) {
		mapping.setDown(physicallyDown(mapping));
	}
}
