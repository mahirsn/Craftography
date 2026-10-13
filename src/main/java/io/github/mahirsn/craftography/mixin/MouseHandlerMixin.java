package io.github.mahirsn.craftography.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.Tripod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	private boolean craftography$inGame() {
		return CameraMode.isOpen() && minecraft.gui.screen() == null && minecraft.gui.overlay() == null && minecraft.player != null;
	}

	/** In camera mode the wheel controls the camera instead of the hotbar. */
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void craftography$scroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (handle == minecraft.getWindow().handle() && craftography$inGame()) {
			double amount = yoffset != 0 ? yoffset : -xoffset;
			if (amount != 0) CraftographyClient.onScroll(amount);
			ci.cancel();
		}
	}

	/** Middle click resets the lens instead of picking a block. */
	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void craftography$middleClick(long handle, MouseButtonInfo button, int action, CallbackInfo ci) {
		if (button.button() == InputConstants.MOUSE_BUTTON_MIDDLE && handle == minecraft.getWindow().handle() && craftography$inGame()) {
			if (action == InputConstants.PRESS) CraftographyClient.onMiddleClick();
			ci.cancel();
		}
	}

	/** Lower mouse sensitivity in proportion to the zoom, and follow the roll. */
	@WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void craftography$scaleTurn(LocalPlayer player, double xo, double yo, Operation<Void> original) {
		// On a tripod the mouse turns the player, not the camera: no zoom or roll correction.
		if (CameraMode.isActive() && !Tripod.isActive()) {
			double base = Math.toRadians(minecraft.options.fov().get()) / 2;
			double current = Math.toRadians(minecraft.gameRenderer.mainCamera().getFov()) / 2;
			double factor = Math.tan(current) / Math.tan(base);
			// Turn in image space: with the camera rolled, moving the mouse right still pans the picture right.
			double a = Math.toRadians(CameraMode.roll()), cos = Math.cos(a), sin = Math.sin(a);
			double x = xo * cos - yo * sin, y = xo * sin + yo * cos;
			xo = x * factor;
			yo = y * factor;
		}
		original.call(player, xo, yo);
	}
}
