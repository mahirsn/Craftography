package io.github.mahirsn.craftography.mixin;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.render.HeldCamcorder;

import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.render.LensEffects;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow @Final private Projection hudProjection;
	@Shadow @Final private SubmitNodeStorage handAndScreenSubmitNodeStorage;
	@Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

	@Inject(method = "extractCamera", at = @At("TAIL"))
	private void craftography$prepare(DeltaTracker delta, float worldTicks, float entityTicks, CallbackInfo ci) {
		if (!CameraMode.isActive()) return;
		var state = ((GameRenderer)(Object)this).gameRenderState();
		HeldCamcorder.prepare(state.levelRenderState.cameraRenderState,
			state.windowRenderState.width, state.windowRenderState.height);
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void craftography$held(CameraRenderState state, float delta, Matrix4fc view, CallbackInfo ci) {
		if (!CameraMode.isActive()) return;
		if (state.isPanoramicMode || HeldCamcorder.isHudHidden()) { ci.cancel(); return; }
		var stack = RenderSystem.getModelViewStack();
		stack.pushMatrix().identity();
		var renderer = ((GameRenderer)(Object)this).itemInHandRenderer;
		HeldCamcorder.submit(renderer, handAndScreenSubmitNodeStorage, hudProjection.getMatrix(new Matrix4f()));
		featureRenderDispatcher.renderAllFeatures(handAndScreenSubmitNodeStorage);
		stack.popMatrix();
		ci.cancel();
	}

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
