package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.Tripod;
import io.github.mahirsn.craftography.util.LensMath;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void setPosition(Vec3 position);

	/** Replaces the field of view with the lens' vertical FOV, blended by {@link CameraMode#view()}. */
	@ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
	private float craftography$lensFov(float fov) {
		if (!CameraMode.isActive()) return fov;
		float lens = (float) LensMath.vfov(CameraMode.focalLength());
		return fov + (lens - fov) * CameraMode.view();
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

	/**
	 * Tripod: after the camera follows the player, put it back on the tripod. Marking it detached makes the level
	 * renderer draw the local player, as in third person. Runs before the culling frustum is built from the position.
	 */
	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void craftography$tripod(float partialTicks, CallbackInfo ci) {
		if (!Tripod.isActive()) return;
		setRotation(Tripod.yRot(), Tripod.xRot());
		setPosition(Tripod.position());
		detached = true;
	}

	/** Tripod: the player's walk bobbing and hurt shake must not move a camera that stands on its own. */
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void craftography$steadyTripod(CameraRenderState state, float partialTicks, CallbackInfo ci) {
		if (!Tripod.isActive()) return;
		state.entityRenderState.bob = 0;
		state.entityRenderState.hurtTime = -1;
	}
}
