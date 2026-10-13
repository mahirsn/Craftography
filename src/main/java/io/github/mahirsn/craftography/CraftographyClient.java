package io.github.mahirsn.craftography;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.screen.ControlsScreen;
import io.github.mahirsn.craftography.util.LensMath;
import io.github.mahirsn.craftography.util.TimeMath;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CraftographyClient implements ClientModInitializer {
	public static final String MOD_ID = "craftography";
	public static final Logger LOG = LoggerFactory.getLogger("Craftography");

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	public static KeyMapping toggleKey;
	public static KeyMapping interactiveKey;
	public static KeyMapping guideKey;
	public static KeyMapping aspectKey;
	public static KeyMapping resetRollKey;
	public static KeyMapping serverTimeKey;
	public static KeyMapping weatherKey;
	public static KeyMapping panelKey;
	public static KeyMapping shutterKey;
	public static KeyMapping timerKey;
	public static KeyMapping tripodKey;
	public static KeyMapping galleryKey;

	/**
	 * Keys that only work in camera mode, with their actions. Vanilla keys bound to the same key are blocked while
	 * camera mode is on. The controls screen runs the same actions when it gets one of these keys.
	 */
	private static final Map<KeyMapping, Runnable> CAMERA_KEYS = new LinkedHashMap<>();

	@Override
	public void onInitializeClient() {
		CameraSettings.load();
		toggleKey = register("toggle", InputConstants.KEY_F7);
		interactiveKey = cameraKey("interactive", InputConstants.KEY_F8, () -> openScreen(false));
		guideKey = cameraKey("guide", InputConstants.UNKNOWN.getValue(), () -> change(CameraSettings::cycleGuide));
		aspectKey = cameraKey("aspect", InputConstants.UNKNOWN.getValue(), () -> change(CameraSettings::cycleAspect));
		resetRollKey = cameraKey("reset_roll", InputConstants.UNKNOWN.getValue(), () -> change(s -> s.roll = 0));
		serverTimeKey = cameraKey("server_time", InputConstants.UNKNOWN.getValue(), () -> change(s -> s.timeOfDay = -1));
		weatherKey = cameraKey("weather", InputConstants.UNKNOWN.getValue(), () -> change(CameraSettings::cycleWeather));
		panelKey = cameraKey("panel", InputConstants.UNKNOWN.getValue(), () -> openScreen(true));
		shutterKey = cameraKey("shutter", InputConstants.UNKNOWN.getValue(), Shutter::press);
		timerKey = cameraKey("timer", InputConstants.UNKNOWN.getValue(), () -> change(CameraSettings::cycleTimer));
		tripodKey = cameraKey("tripod", InputConstants.KEY_F9, Tripod::toggle);
		galleryKey = register("gallery", InputConstants.UNKNOWN.getValue());

		ClientTickEvents.START_CLIENT_TICK.register(CraftographyClient::onTick);
	}

	private static KeyMapping register(String name, int key) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.craftography." + name, InputConstants.Type.KEYBOARD, key, CATEGORY));
	}

	private static KeyMapping cameraKey(String name, int key, Runnable action) {
		KeyMapping mapping = register(name, key);
		CAMERA_KEYS.put(mapping, action);
		return mapping;
	}

	/** Changes a setting and saves it. */
	public static void change(Consumer<CameraSettings> change) {
		change.accept(CameraSettings.get());
		CameraSettings.save();
	}

	private static void openScreen(boolean held) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() == null) mc.gui.setScreen(new ControlsScreen(held));
	}

	/** Runs the camera action bound to a key pressed while a screen is open. Returns false if no action matches. */
	public static boolean runCameraKey(KeyEvent event) {
		for (Map.Entry<KeyMapping, Runnable> e : CAMERA_KEYS.entrySet()) {
			if (!e.getKey().isUnbound() && e.getKey().matches(event)) {
				e.getValue().run();
				return true;
			}
		}
		return false;
	}

	/** True in camera mode when {@code key} is bound to a camera action. */
	public static boolean isCameraKey(InputConstants.Key key) {
		if (!CameraMode.isOpen()) return false;
		for (KeyMapping mapping : CAMERA_KEYS.keySet()) {
			if (!mapping.isUnbound() && mapping.matches(key)) return true;
		}
		return false;
	}

	/** Runs before vanilla handles its key bindings in the same tick. */
	private static void onTick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			CameraMode.reset();
			Shutter.cancelTimer();
			Tripod.release();
			return;
		}
		while (toggleKey.consumeClick()) {
			CameraMode.toggle();
		}
		Shutter.tickHiRes(mc);
		while (galleryKey.consumeClick()) {
			if (mc.gui.screen() == null) mc.gui.setScreen(new io.github.mahirsn.craftography.screen.GalleryScreen(null));
		}
		boolean open = CameraMode.isOpen();
		if (!open) {
			Shutter.cancelTimer();
			Tripod.release();
		}
		Tripod.tick(mc);
		Autofocus.tick(mc);
		for (Map.Entry<KeyMapping, Runnable> e : CAMERA_KEYS.entrySet()) {
			while (e.getKey().consumeClick()) {
				// Not in camera mode: the click is dropped.
				if (open) e.getValue().run();
			}
		}
		if (!open) return;
		Shutter.tick();
		blockVanillaKeys(mc);
	}

	public static boolean isDown(int key) {
		return InputConstants.isKeyDown(key);
	}

	static boolean altDown() {
		return isDown(InputConstants.KEY_LALT) || isDown(InputConstants.KEY_RALT);
	}

	static boolean ctrlDown() {
		return isDown(InputConstants.KEY_LCONTROL) || isDown(InputConstants.KEY_RCONTROL);
	}

	public static boolean shiftDown() {
		return isDown(InputConstants.KEY_LSHIFT) || isDown(InputConstants.KEY_RSHIFT);
	}

	/** Mouse wheel in camera mode. {@code amount} is positive when scrolling up. */
	public static void onScroll(double amount) {
		CameraSettings s = CameraSettings.get();
		if (ctrlDown()) {
			int start = s.timeOfDay >= 0 ? s.timeOfDay : TimeMath.dayTicks(CameraMode.serverClockTicks);
			s.timeOfDay = TimeMath.dayTicks(start + Math.round(amount * (shiftDown() ? 10 : 100)));
		} else if (altDown()) {
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
			for (KeyMapping key : CAMERA_KEYS.keySet()) {
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
