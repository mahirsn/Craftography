package io.github.mahirsn.craftography.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mahirsn.craftography.CameraSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Visual weather: only the client level is changed, never a server level. */
@Mixin(Level.class)
public abstract class LevelMixin {
	private CameraSettings.Weather craftography$weather() {
		if (!((Object) this instanceof ClientLevel level) || !level.canHaveWeather()) return CameraSettings.Weather.SERVER;
		CameraSettings s = CameraSettings.get();
		return s.overridesApply() ? s.weather : CameraSettings.Weather.SERVER;
	}

	@ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
	private float craftography$rain(float rain) {
		return switch (craftography$weather()) {
			case SERVER -> rain;
			case CLEAR -> 0f;
			case RAIN, THUNDER -> 1f;
		};
	}

	@ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"))
	private float craftography$thunder(float thunder) {
		return switch (craftography$weather()) {
			case SERVER -> thunder;
			case CLEAR, RAIN -> 0f;
			case THUNDER -> 1f;
		};
	}
}
