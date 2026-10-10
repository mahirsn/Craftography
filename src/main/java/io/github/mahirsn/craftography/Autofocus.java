package io.github.mahirsn.craftography;

import io.github.mahirsn.craftography.util.ExposureMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Where the lens is focused. With autofocus on, it measures the distance to whatever is in the frame centre
 * (blocks and entities, the player too when the camera is on a tripod) and drives the focus there like a lens
 * motor. With autofocus off, the focus ring setting is used.
 */
public final class Autofocus {
	private static final double RANGE = 256;
	private static double motor = ExposureMath.FOCUS_INFINITY;

	private Autofocus() {
	}

	/** The distance the lens is focused at now, in metres. */
	public static double distance() {
		CameraSettings s = CameraSettings.get();
		return s.autofocus ? motor : s.focusDistance;
	}

	public static void tick(Minecraft mc) {
		if (!CameraMode.isActive() || !CameraSettings.get().autofocus || mc.level == null) return;
		double target = measure(mc);
		// The motor covers half the remaining way each tick, in log space (equal steps near and far).
		motor = Math.exp((Math.log(motor) + Math.log(target)) / 2);
		if (Math.abs(Math.log(motor / target)) < 0.01) motor = target;
	}

	/** Distance along the view axis to the first block or entity in the frame centre, or infinity. */
	public static double measure(Minecraft mc) {
		Camera camera = mc.gameRenderer.mainCamera();
		Vec3 from = camera.position();
		Vec3 dir = new Vec3(camera.forwardVector());
		Vec3 to = from.add(dir.scale(RANGE));
		double best = RANGE;
		HitResult block = mc.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.player));
		if (block.getType() != HitResult.Type.MISS) best = block.getLocation().distanceTo(from);
		boolean tripod = Tripod.isActive();
		AABB area = new AABB(from, to).inflate(1);
		for (Entity e : mc.level.getEntities((Entity) null, area, e -> e.isPickable() || e == mc.player)) {
			if (e == mc.player && !tripod) continue; // the camera is in the player's head
			var hit = e.getBoundingBox().inflate(e.getPickRadius()).clip(from, to);
			if (hit.isPresent()) best = Math.min(best, hit.get().distanceTo(from));
		}
		return best >= RANGE ? ExposureMath.FOCUS_INFINITY : Math.max(ExposureMath.MIN_FOCUS, best);
	}
}
