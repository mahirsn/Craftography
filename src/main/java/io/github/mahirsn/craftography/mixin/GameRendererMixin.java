package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.mahirsn.craftography.Lean;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.render.HeldCamera;
import io.github.mahirsn.craftography.render.LensEffects;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	/**
	 * After the level (and any shader pack pass) is drawn and before the GUI: runs the lens effects, then takes a
	 * requested photo, so the photo is exactly what the viewfinder shows.
	 */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
	private void craftography$capture(CallbackInfo ci) {
		LensEffects.apply(((GameRenderer) (Object) this).mainRenderTarget());
		// Only once leaned in: the camcorder is gone and the effects are at full strength, as the viewfinder shows.
		if (Shutter.isPending() && Lean.full()) Shutter.capture();
		LensEffects.liveView(((GameRenderer) (Object) this).mainRenderTarget());
	}

	/** Camera mode hides the HUD as F1 does, which also hides the hand; the held camcorder is drawn anyway. */
	@ModifyExpressionValue(method = "renderItemInHand", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;isHudHidden:Z"))
	private boolean craftography$showHeldCamera(boolean hidden) {
		return hidden && !HeldCamera.visible();
	}

	/** Keeps the level depth for depth of field before vanilla clears it for the hand. */
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", shift = At.Shift.AFTER))
	private void craftography$keepDepth(CallbackInfo ci) {
		LensEffects.keepDepth(((GameRenderer) (Object) this).mainRenderTarget());
	}
}
