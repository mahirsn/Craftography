package io.github.mahirsn.craftography.mixin;

import io.github.mahirsn.craftography.capture.Shutter;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	/** Takes a requested photo after the level (and any shader pack pass) is drawn, before the GUI. */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
	private void craftography$capture(CallbackInfo ci) {
		if (Shutter.isPending()) Shutter.capture();
	}
}
