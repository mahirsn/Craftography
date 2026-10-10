package io.github.mahirsn.craftography;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mahirsn.craftography.util.LensMath;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CraftographyClient implements ClientModInitializer {
	public static final String MOD_ID = "craftography";
	public static final Logger LOG = LoggerFactory.getLogger("Craftography");

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	public static KeyMapping toggleKey;
	public static KeyMapping guideKey;
	public static KeyMapping aspectKey;
	public static KeyMapping resetRollKey;

	/** Keys that only work in camera mode; vanilla keys bound to the same key are blocked while camera mode is on. */
	private static List<KeyMapping> cameraKeys;

	@Override
	public void onInitializeClient() {
		CameraSettings.load();
		toggleKey = register("toggle", GLFW.GLFW_KEY_V);
		guideKey = register("guide", GLFW.GLFW_KEY_G);
		aspectKey = register("aspect", GLFW.GLFW_KEY_B);
		resetRollKey = register("reset_roll", GLFW.GLFW_KEY_R);
		cameraKeys = List.of(guideKey, aspectKey, resetRollKey);

		ClientTickEvents.START_CLIENT_TICK.register(CraftographyClient::onTick);
	}

	private static KeyMapping register(String name, int key) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.craftography." + name, InputConstants.Type.KEYSYM, key, CATEGORY));
	}

	/** Runs before vanilla handles its key bindings in the same tick. */
	private static void onTick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			CameraMode.reset();
			return;
		}
		while (toggleKey.consumeClick()) {
			CameraMode.toggle();
		}
		if (!CameraMode.isOpen()) {
			for (KeyMapping key : cameraKeys) {
				while (key.consumeClick()) {
					// Not in camera mode: ignore.
				}
			}
			return;
		}

		boolean changed = false;
		while (guideKey.consumeClick()) {
			CameraSettings.get().cycleGuide();
			changed = true;
		}
		while (aspectKey.consumeClick()) {
			CameraSettings.get().cycleAspect();
			changed = true;
		}
		while (resetRollKey.consumeClick()) {
			CameraSettings.get().roll = 0;
			changed = true;
		}
		if (changed) CameraSettings.save();
		blockVanillaKeys(mc);
	}

	static boolean isDown(int key) {
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
	}

	static boolean altDown() {
		return isDown(GLFW.GLFW_KEY_LEFT_ALT) || isDown(GLFW.GLFW_KEY_RIGHT_ALT);
	}

	static boolean shiftDown() {
		return isDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	/** Mouse wheel in camera mode. {@code amount} is positive when scrolling up. */
	public static void onScroll(double amount) {
		CameraSettings s = CameraSettings.get();
		if (altDown()) {
			s.addRoll(amount * (shiftDown() ? 0.1 : 1));
		} else {
			s.focalLength = LensMath.zoom(s.focalLength, shiftDown() ? amount / 4 : amount);
		}
		CameraSettings.save();
	}

	public static void onMiddleClick() {
		CameraSettings.get().focalLength = LensMath.DEFAULT_MM;
		CameraSettings.save();
	}

	/** Stops vanilla actions that share a key with a camera key (for example G = quick actions). */
	private static void blockVanillaKeys(Minecraft mc) {
		for (KeyMapping vanilla : mc.options.keyMappings) {
			if (vanilla.getCategory() == CATEGORY) continue;
			for (KeyMapping key : cameraKeys) {
				if (!key.isUnbound() && vanilla.same(key)) {
					while (vanilla.consumeClick()) {
						// Drop the click.
					}
					if (key.isDown()) vanilla.setDown(false);
				}
			}
		}
	}
}
