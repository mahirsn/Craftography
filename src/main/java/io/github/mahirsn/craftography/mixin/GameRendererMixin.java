package io.github.mahirsn.craftography.mixin;

import io.github.mahirsn.craftography.capture.Shutter;
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
		if (Shutter.isPending()) Shutter.capture();
		LensEffects.liveView(((GameRenderer) (Object) this).mainRenderTarget());
	}

	/** Keeps the level depth for depth of field before vanilla clears it for the hand. */
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", shift = At.Shift.AFTER))
	private void craftography$keepDepth(CallbackInfo ci) {
		LensEffects.keepDepth(((GameRenderer) (Object) this).mainRenderTarget());
	}
}
