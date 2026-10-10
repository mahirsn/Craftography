package io.github.mahirsn.craftography.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mahirsn.craftography.CraftographyClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/**
	 * Vanilla runs its global keys (fullscreen, screenshot, friends) before any key binding sees the press. In camera
	 * mode a camera key on the same key wins, as for every other vanilla key; with camera mode off nothing changes.
	 */
	@Inject(method = "handleGlobalKeyPress", at = @At("HEAD"), cancellable = true)
	private void craftography$cameraKeyFirst(InputConstants.Key key, boolean controlDown, CallbackInfoReturnable<Boolean> cir) {
		if (CraftographyClient.isCameraKey(key)) cir.setReturnValue(false);
	}
}
