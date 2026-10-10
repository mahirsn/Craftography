package io.github.mahirsn.craftography;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Camera on a tripod: the view stays at a fixed position and rotation while the player walks freely, and the
 * player's own model is drawn. Client-side only: it changes nothing but where the client's camera renders from.
 */
public final class Tripod {
	private static Vec3 position;
	private static float yRot, xRot;
	private static Level level;

	private Tripod() {
	}

	public static boolean isActive() {
		return position != null;
	}

	public static Vec3 position() {
		return position;
	}

	public static float yRot() {
		return yRot;
	}

	public static float xRot() {
		return xRot;
	}

	/** Sets the tripod down where the camera is now, or gives the camera back to the player. */
	public static void toggle() {
		if (isActive()) {
			release();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.mainCamera();
		position = camera.position();
		yRot = camera.yRot();
		xRot = camera.xRot();
		level = mc.level;
	}

	public static void release() {
		position = null;
		level = null;
	}

	/** Releases the tripod when the player changes dimension or leaves the world. */
	public static void tick(Minecraft mc) {
		if (isActive() && mc.level != level) release();
	}
}
