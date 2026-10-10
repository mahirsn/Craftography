package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.util.LensMath;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin {
	/** Replaces the field of view with the lens' vertical FOV, blended by the open/close transition. */
	@ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
	private float craftography$lensFov(float fov) {
		if (!CameraMode.isActive()) return fov;
		float lens = (float) LensMath.vfov(CameraMode.focalLength());
		return fov + (lens - fov) * CameraMode.progress();
	}
}
