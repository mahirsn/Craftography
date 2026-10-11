package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.hud.ViewfinderHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In camera mode the vanilla HUD, hand, block outline and name tags behave as if F1 was pressed,
 * and the viewfinder is drawn instead. F1 itself still hides the viewfinder.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
	@Shadow
	private boolean isHidden;

	@Inject(method = "isHidden", at = @At("HEAD"), cancellable = true)
	private void craftography$hideInCameraMode(CallbackInfoReturnable<Boolean> cir) {
		if (CameraMode.isActive()) cir.setReturnValue(true);
	}

	@ModifyExpressionValue(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Hud;isHidden:Z"))
	private boolean craftography$hideVanillaHud(boolean hidden) {
		return hidden || CameraMode.isActive();
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void craftography$drawViewfinder(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (CameraMode.isActive() && !this.isHidden) ViewfinderHud.render(graphics, deltaTracker);
	}
}
