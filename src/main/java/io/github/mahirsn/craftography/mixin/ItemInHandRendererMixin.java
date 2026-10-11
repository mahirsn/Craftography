package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mahirsn.craftography.render.HeldCamera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In camera mode the first-person hand shows the camcorder, and nothing in the other hand. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@ModifyExpressionValue(method = "submitHandsWithItems", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;mainHandItem:Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack craftography$camcorder(ItemStack held) {
		return HeldCamera.visible() ? HeldCamera.stack() : held;
	}

	@ModifyExpressionValue(method = "submitHandsWithItems", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;offHandItem:Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack craftography$emptyOffHand(ItemStack held) {
		return HeldCamera.visible() ? ItemStack.EMPTY : held;
	}

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"))
	private void craftography$pose(float partialTicks, PoseStack pose, SubmitNodeCollector collector, LocalPlayer player, int light, CallbackInfo ci) {
		if (HeldCamera.visible()) HeldCamera.pose(pose);
	}
}
