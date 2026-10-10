package io.github.mahirsn.craftography;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraftographyClient implements ClientModInitializer {
	public static final String MOD_ID = "craftography";
	public static final Logger LOG = LoggerFactory.getLogger("Craftography");

	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	public static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.craftography.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				LOG.info("Camera mode toggle pressed");
			}
		});
	}
}
