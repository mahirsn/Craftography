package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.util.OverworldClock;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientClockManager.ClientClockInstance.class)
public abstract class ClientClockInstanceMixin implements OverworldClock {
	@Unique
	private boolean craftography$overworld;

	@Override
	public void craftography$markOverworld() {
		craftography$overworld = true;
	}

	@ModifyReturnValue(method = "totalTicks", at = @At("RETURN"))
	private long craftography$visibleTime(long ticks) {
		if (!craftography$overworld) return ticks;
		CameraMode.serverClockTicks = ticks;
		CameraSettings s = CameraSettings.get();
		if (s.timeOfDay < 0 && s.moonPhase < 0 || !s.overridesApply()) return ticks;
		return TimeMath.visible(ticks, s.timeOfDay, s.moonPhase);
	}
}
