package io.github.mahirsn.craftography.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import io.github.mahirsn.craftography.Autofocus;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.hud.ViewfinderHud;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.Optional;

/**
 * The camera's image processing, run on the finished frame before the GUI and before a photo is taken, so the
 * viewfinder and the saved photo show the same picture:
 * <ol>
 * <li>lens pass: depth of field from the level depth, exposure (ISO, aperture, shutter), white balance, and
 * motion blur from a slow shutter (blend with the previous output);</li>
 * <li>grain pass: sensor noise that grows with ISO.</li>
 * </ol>
 * Everything except depth of field only changes colours of the final image, so it also works on top of a shader
 * pack. Depth of field needs the level depth, which a shader pack does not leave in the main target: it is off
 * while a pack is in use.
 */
public final class LensEffects {
	private static final RenderPipeline LENS = pipeline("lens", "Scene", "Depth", "History");
	private static final RenderPipeline GRAIN = pipeline("grain", "In");
	private static final RenderPipeline DOWNSAMPLE = simplePipeline("downsample");
	private static final RenderPipeline PEAKING = simplePipeline("peaking");
	private static final RenderPipeline COMPOSITE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "pipeline/viewfinder"))
		.withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
		.withFragmentShader(Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "post/viewfinder"))
		.withBindGroupLayout(BindGroupLayout.builder().withSampler("InSampler").withSampler("OriginalSampler")
			.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER).build()).build();
	private static final int INFO_SIZE = new Std140SizeCalculator().putVec2().putVec2().putVec4().get();
	private static final int UBO_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec4().putVec4().putVec4().get();

	private static MappableRingBuffer config, samplerInfo;
	private static TextureTarget scene, depth, history, output, small, untreated;
	private static boolean historyValid;
	private static long lastFrameNanos;
	private static int frame;

	private LensEffects() {
	}

	private static RenderPipeline pipeline(String name, String... samplers) {
		BindGroupLayout.Builder layout = BindGroupLayout.builder();
		for (String sampler : samplers) layout.withSampler(sampler + "Sampler");
		layout.withUniform("LensConfig", UniformType.UNIFORM_BUFFER);
		return RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "pipeline/" + name))
			.withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
			.withFragmentShader(Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "post/" + name))
			.withBindGroupLayout(layout.build())
			.build();
	}

	/** A one-input pass with vanilla's SamplerInfo uniform (output and input size). */
	private static RenderPipeline simplePipeline(String name) {
		return RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "pipeline/" + name))
			.withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
			.withFragmentShader(Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "post/" + name))
			.withBindGroupLayout(BindGroupLayout.builder().withSampler("InSampler").withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER).build())
			.build();
	}

	private static void simplePass(String label, RenderPipeline pipeline, TextureTarget in, RenderTarget out, FilterMode filter) {
		if (samplerInfo == null) samplerInfo = new MappableRingBuffer(() -> "Craftography sampler info", 130, INFO_SIZE);
		try (GpuBufferSlice.MappedView view = samplerInfo.currentBuffer().map(false, true)) {
			Std140Builder.intoBuffer(view.data()).putVec2(out.width, out.height).putVec2(in.width, in.height);
		}
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> label, out.getColorTextureView(), Optional.empty())) {
			pass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("SamplerInfo", samplerInfo.currentBuffer());
			pass.bindTexture("InSampler", in.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(filter));
			pass.draw(3, 1, 0, 0);
		}
		samplerInfo.rotate();
	}

	/**
	 * Called after a photo is taken and before the GUI: live-view aids that must not be in the photo (focus
	 * peaking), and the light meter, which reads the picture as the photo would be.
	 */
	public static void liveView(RenderTarget main) {
		if (!CameraMode.isActive()) return;
		Meter.request(() -> {
			small = fitSize(small, "meter", Meter.WIDTH, Meter.HEIGHT);
			simplePass("Craftography meter", DOWNSAMPLE, copyOfMain(main), small, FilterMode.LINEAR);
			return small;
		});
		if (CameraSettings.get().peaking) simplePass("Craftography focus peaking", PEAKING, copyOfMain(main), main, FilterMode.NEAREST);
		restoreSurroundings(main);
	}

	/** Restore the untouched world outside the animated LCD, after all photo and live-view processing. */
	private static void restoreSurroundings(RenderTarget main) {
		if (untreated == null) return;
		var window = Minecraft.getInstance().getWindow();
		var lcd = ViewfinderHud.liveView(window.getGuiScaledWidth(), window.getGuiScaledHeight()).scale(window.getGuiScale());
		TextureTarget processed = copyOfMain(main);
		if (samplerInfo == null) samplerInfo = new MappableRingBuffer(() -> "Craftography sampler info", 130, INFO_SIZE);
		try (GpuBufferSlice.MappedView view = samplerInfo.currentBuffer().map(false, true)) {
			Std140Builder.intoBuffer(view.data()).putVec2(main.width, main.height).putVec2(main.width, main.height)
				.putVec4((float) lcd.x() / main.width, 1 - (float) lcd.bottom() / main.height,
					(float) lcd.right() / main.width, 1 - (float) lcd.y() / main.height);
		}
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Craftography LCD composite", main.getColorTextureView(), Optional.empty())) {
			pass.setPipeline(COMPOSITE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("SamplerInfo", samplerInfo.currentBuffer());
			var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
			pass.bindTexture("InSampler", processed.getColorTextureView(), sampler);
			pass.bindTexture("OriginalSampler", untreated.getColorTextureView(), sampler);
			pass.draw(3, 1, 0, 0);
		}
		samplerInfo.rotate();
	}

	private static TextureTarget copyOfMain(RenderTarget main) {
		scene = fit(scene, "scene", main, false);
		RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(main.getColorTexture(), scene.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
		return scene;
	}

	private static TextureTarget fitSize(TextureTarget target, String name, int width, int height) {
		if (target == null) return new TextureTarget("Craftography " + name, width, height, false, GpuFormat.RGBA8_UNORM);
		return target;
	}

	/** True while a shader pack draws the level (Iris), found without a compile-time dependency. */
	public static boolean shaderPackInUse() {
		if (!FabricLoader.getInstance().isModLoaded("iris")) return false;
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Object instance = api.getMethod("getInstance").invoke(null);
			return (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}

	/** Whether depth of field can work now. */
	public static boolean depthOfFieldAvailable() {
		return !shaderPackInUse();
	}

	private static TextureTarget fit(TextureTarget target, String name, RenderTarget main, boolean useDepth) {
		if (target == null) return new TextureTarget("Craftography " + name, main.width, main.height, useDepth, GpuFormat.RGBA8_UNORM);
		if (target.width != main.width || target.height != main.height) {
			target.resize(main.width, main.height);
			historyValid = false;
		}
		return target;
	}

	/** Called right after the level is drawn, before vanilla clears the depth for the hand: keeps the depth. */
	public static void keepDepth(RenderTarget main) {
		if (!CameraMode.isActive() || !depthOfFieldAvailable() || main.getDepthTexture() == null) return;
		depth = fit(depth, "depth", main, true);
		depth.copyDepthFrom(main);
	}

	/** Called before the GUI and before a photo is taken. */
	public static void apply(RenderTarget main) {
		if (!CameraMode.isActive()) {
			historyValid = false;
			return;
		}
		if (config == null) config = new MappableRingBuffer(() -> "Craftography lens config", 130, UBO_SIZE);
		scene = fit(scene, "scene", main, false);
		history = fit(history, "history", main, false);
		output = fit(output, "output", main, false);
		depth = fit(depth, "depth", main, true);

		long now = System.nanoTime();
		double frameSeconds = lastFrameNanos == 0 ? 1 / 60.0 : Math.min(0.25, (now - lastFrameNanos) / 1e9);
		lastFrameNanos = now;
		writeConfig(main, frameSeconds);
		untreated = fit(untreated, "untreated surroundings", main, false);

		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		encoder.copyTextureToTexture(main.getColorTexture(), untreated.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
		encoder.copyTextureToTexture(main.getColorTexture(), scene.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
		var linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		var nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
		try (RenderPass pass = encoder.createRenderPass(() -> "Craftography lens", output.getColorTextureView(), Optional.empty())) {
			pass.setPipeline(LENS);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("LensConfig", config.currentBuffer());
			pass.bindTexture("SceneSampler", scene.getColorTextureView(), linear);
			pass.bindTexture("DepthSampler", depth.getDepthTextureView(), nearest);
			pass.bindTexture("HistorySampler", history.getColorTextureView(), nearest);
			pass.draw(3, 1, 0, 0);
		}
		try (RenderPass pass = encoder.createRenderPass(() -> "Craftography grain", main.getColorTextureView(), Optional.empty())) {
			pass.setPipeline(GRAIN);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("LensConfig", config.currentBuffer());
			pass.bindTexture("InSampler", output.getColorTextureView(), nearest);
			pass.draw(3, 1, 0, 0);
		}
		config.rotate();
		// The output before grain is the history of the next frame.
		TextureTarget swap = history;
		history = output;
		output = swap;
		historyValid = true;
		frame++;
	}

	private static void writeConfig(RenderTarget main, double frameSeconds) {
		CameraSettings s = CameraSettings.get();
		float p = CameraMode.progress();
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.mainCamera();
		// Projection = (projection * view rotation) * inverse(view rotation).
		Matrix4f viewRotation = camera.getViewRotationMatrix(new Matrix4f());
		Matrix4f inverseProjection = camera.getViewRotationProjectionMatrix(new Matrix4f()).mul(viewRotation.invert()).invert();

		// Every effect fades in and out with the camera mode transition.
		double gain = Math.pow(ExposureMath.gain(s.aperture, s.shutter, s.iso), p);
		double[] wb = ExposureMath.whiteBalance(s.kelvin, s.tint);
		// No noise at base ISO, so the defaults leave the picture unchanged.
		float grain = s.noise ? (float) (0.0045 * (Math.sqrt(s.iso / 100.0) - 1) * p) : 0;
		boolean dof = depthOfFieldAvailable();
		double focus = Autofocus.distance();
		float cocPerInverseMetre = (float) (ExposureMath.cocScale(CameraMode.focalLength(), s.aperture, focus) * main.height * p);
		float maxRadius = Math.max(1, main.height / 45f);
		// Slow shutter: keep the share of the light that the previous frames collected.
		float previous = historyValid ? (float) Math.clamp(1 - frameSeconds / s.shutter, 0, 0.97) * p : 0;
		boolean zeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();

		try (GpuBufferSlice.MappedView view = config.currentBuffer().map(false, true)) {
			Std140Builder.intoBuffer(view.data())
				.putMat4f(inverseProjection)
				.putVec4((float) (lerp(1, wb[0], p) * gain), (float) (lerp(1, wb[1], p) * gain), (float) (lerp(1, wb[2], p) * gain), grain)
				.putVec4(cocPerInverseMetre, (float) ExposureMath.inverseFocus(focus), maxRadius, dof ? 1 : 0)
				.putVec4(main.width, main.height, previous, frame % 1024)
				.putVec4(zeroToOne ? 1 : 0, 0, 0, 0);
		}
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}
}
