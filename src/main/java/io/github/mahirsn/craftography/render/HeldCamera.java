package io.github.mahirsn.craftography.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.Lean;
import io.github.mahirsn.craftography.Tripod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The camcorder held in first person while camera mode is on. It is only drawn: the player's real items are not
 * touched and nothing is sent to the server, so other players see what the player really holds. The model is
 * assets/craftography/items/camcorder.json; its LCD screen is see-through.
 */
public final class HeldCamera {
	private static ItemStack stack;
	/** The player's own F1 state; camera mode hides the vanilla HUD, but F1 should still hide the camera. */
	public static boolean hiddenByUser;

	// Lean pose: how far the camera moves toward the eye (view space, blocks) and turns so its screen faces the eye.
	// ponytail: tuned by eye on the current model; derive from the LCD corners if the model changes a lot.
	private static final float LEAN_X = -0.30f, LEAN_Y = 0.22f, LEAN_Z = 0.42f, LEAN_YAW = 0f, LEAN_PITCH = 0f;

	private HeldCamera() {
	}

	public static ItemStack stack() {
		if (stack == null) {
			stack = new ItemStack(Items.PAPER);
			stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "camcorder"));
		}
		return stack;
	}

	/** Whether the hand pass draws the camcorder now. Once fully leaned in, the viewfinder HUD replaces it. */
	public static boolean visible() {
		return CameraMode.isActive() && CameraSettings.get().heldCamera && !Tripod.isActive() && !hiddenByUser && Lean.value() < 1f;
	}

	/** Raises the camera as camera mode opens and brings it to the eye as the player leans in. */
	public static void pose(PoseStack pose) {
		float lean = Lean.value(), lowered = 1 - CameraMode.progress();
		pose.translate(LEAN_X * lean, LEAN_Y * lean - 0.9f * lowered, LEAN_Z * lean);
		pose.mulPose(Axis.YP.rotationDegrees(LEAN_YAW * lean));
		pose.mulPose(Axis.XP.rotationDegrees(LEAN_PITCH * lean));
	}
}
