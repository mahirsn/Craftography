package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mahirsn.craftography.util.OverworldClock;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Changes only the time the client sees. ClientClockManager is the client's own copy of the world clocks: the server
 * (also the integrated one) keeps its own ServerClockManager, and clock packets still update the real value here.
 * Every reader gets the time through getInstance, so the overworld instance is marked here and its totalTicks()
 * is changed in ClientClockInstanceMixin.
 */
@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin {
	@ModifyReturnValue(method = "getInstance(Lnet/minecraft/core/Holder;)Lnet/minecraft/client/ClientClockManager$ClientClockInstance;", at = @At("RETURN"))
	private ClientClockManager.ClientClockInstance craftography$markOverworld(ClientClockManager.ClientClockInstance instance, Holder<WorldClock> definition) {
		if (definition.is(WorldClocks.OVERWORLD)) ((OverworldClock) instance).craftography$markOverworld();
		return instance;
	}
}
