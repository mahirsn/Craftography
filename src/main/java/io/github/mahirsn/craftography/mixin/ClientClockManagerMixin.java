package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Changes only the time the client sees. ClientClockManager is the client's own copy of the world clocks: the server
 * (also the integrated one) keeps its own ServerClockManager, and clock packets still update the real value here.
 */
@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin {
	@ModifyReturnValue(method = "getTotalTicks", at = @At("RETURN"))
	private long craftography$visibleTime(long ticks, Holder<WorldClock> definition) {
		if (!definition.is(WorldClocks.OVERWORLD)) return ticks;
		CameraMode.serverClockTicks = ticks;
		CameraSettings s = CameraSettings.get();
		if (s.timeOfDay < 0 && s.moonPhase < 0 || !s.overridesApply()) return ticks;
		return TimeMath.visible(ticks, s.timeOfDay, s.moonPhase);
	}
}
