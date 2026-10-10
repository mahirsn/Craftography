package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.util.LensMath;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
	/** Replaces the field of view with the lens' vertical FOV, blended by the open/close transition. */
	@ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
	private float craftography$lensFov(float fov) {
		if (!CameraMode.isActive()) return fov;
		float lens = (float) LensMath.vfov(CameraMode.focalLength());
		return fov + (lens - fov) * CameraMode.progress();
	}

	/**
	 * Adds roll to the camera rotation itself, so the view matrix, frustum culling and shader packs all see the same
	 * rotation. The Z angle of rotationYXZ is always 0 in vanilla.
	 */
	@ModifyArg(method = "setRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"), index = 2)
	private float craftography$roll(float z) {
		if (!CameraMode.isActive()) return z;
		return z - (float) Math.toRadians(CameraMode.roll());
	}
}
