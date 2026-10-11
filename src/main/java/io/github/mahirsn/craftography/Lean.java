package io.github.mahirsn.craftography;

import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.screen.ControlsScreen;
import io.github.mahirsn.craftography.util.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/**
 * How far the held camcorder is brought up to the eye: 0 = held in front of the chest, 1 = looking at its screen,
 * where the viewfinder HUD takes over. The lens effects, lens FOV, roll and HUD fade in with it, so holding the camera
 * costs no extra render passes.
 */
public final class Lean {
	public enum Mode {
		/** Leaned in while the key is held. */
		HOLD,
		/** Each press leans in or out. */
		TOGGLE,
		/** A tap toggles; holding the key leans in until it is let go. */
		BOTH;

		public String translationKey() {
			return "craftography.lean." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** A press shorter than this is a tap. */
	private static final long TAP_MS = 250;
	private static final float DURATION_S = 0.22f;

	private static boolean keyDown, toggled;
	private static long pressedAt;
	private static float raw;
	private static long lastNanos;

	private Lean() {
	}

	/**
	 * A press of the lean key, from the key's click queue: a quick tap can start and end between two ticks, so presses
	 * are not read from the held state.
	 */
	public static void press() {
		pressedAt = Util.getMillis();
		if (CameraSettings.get().leanMode != Mode.HOLD) toggled = !toggled;
	}

	/** Called every client tick with whether the lean key is held. */
	public static void tick(boolean down) {
		if (!CameraMode.isOpen()) {
			toggled = false;
		} else if (!down && keyDown && CameraSettings.get().leanMode == Mode.BOTH && Util.getMillis() - pressedAt >= TAP_MS) {
			// Held, not tapped: letting go leans back out.
			toggled = !toggled;
		}
		keyDown = down;
	}

	private static boolean wanted() {
		if (!CameraMode.isOpen()) return false;
		CameraSettings s = CameraSettings.get();
		// Without the held camera, on a tripod, for a photo and in the controls screen the view is the viewfinder.
		if (!s.heldCamera || Tripod.isActive() || Shutter.isPending() || Minecraft.getInstance().gui.screen() instanceof ControlsScreen) return true;
		return s.leanMode == Mode.HOLD ? keyDown : toggled;
	}

	/** Eased lean, 0..1. */
	public static float value() {
		long now = System.nanoTime();
		float seconds = lastNanos == 0 ? 0 : Math.min(0.1f, (now - lastNanos) / 1e9f);
		lastNanos = now;
		raw = Math.clamp(raw + (wanted() ? 1 : -1) * seconds / DURATION_S, 0f, 1f);
		return Easing.inOutCubic(raw);
	}

	/** True when looking at the camera screen: a photo is only taken then, so it shows what the viewfinder shows. */
	public static boolean full() {
		return value() >= 1f;
	}
}
