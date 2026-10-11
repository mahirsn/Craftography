package io.github.mahirsn.craftography.render;

import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import java.util.List;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Client-only prop. No item, inventory change or server component is registered. */
public final class HeldCamcorder {
	private static final ItemStack STACK = new ItemStack(Items.PAPER);
	private static float frameProgress;
	private static int guiWidth, guiHeight;
	private static int submittedQuads;
	public static int submittedQuads() { return submittedQuads; }
	private static boolean hudHidden;
	public static void setHudHidden(boolean hidden) { hudHidden = hidden; }
	public static boolean isHudHidden() { return hudHidden; }
	private static final Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70), 16f/9, .05f, 100);
	static { STACK.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("craftography", "camcorder")); }
	private HeldCamcorder() { }

	// The same pose drives the submitted model and all projected hardware/LCD corners.
	public static Matrix4f pose(double width, double height) {
		float aspect = (float)(width/height);
		float fit = Math.min(1, aspect/(16f/9));
		// Held at an angle so the open LCD, the left side panel and the rear panel all face the eye.
		return new Matrix4f().translate(.48f*fit, -.7f*fit-(1-frameProgress)*2.4f, -2.85f)
			.scale(-1.8f*fit, 1.8f*fit, 1.8f*fit)
			.rotateX((float)Math.toRadians(8)).rotateY((float)Math.toRadians(-112.5));
	}
	public static void prepare(CameraRenderState state, int width, int height) {
		frameProgress = CameraMode.progress();
		submittedQuads = 0;
		var window = Minecraft.getInstance().getWindow();
		guiWidth = window.getGuiScaledWidth();
		guiHeight = window.getGuiScaledHeight();
		var hand = new Projection();
		hand.setupPerspective(.05f,100,state.hudFov,width,height);
		hand.getMatrix(projection);
	}
	public static void submit(ItemInHandRenderer renderer, SubmitNodeCollector collector, Matrix4fc handProjection) {
		projection.set(handProjection);
		var mc = Minecraft.getInstance();
		var window = mc.getWindow();
		PoseStack poses = new PoseStack();
		poses.mulPose(pose(window.getGuiScaledWidth(), window.getGuiScaledHeight()));
		renderer.renderItem(mc.player, STACK, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, poses,
			new SubmitNodeStorage() {
				@Override public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay,
					int outline, int[] tints, List<BakedQuad> quads,
					FoilType foil) {
					submittedQuads += quads.size();
					// Reflection puts the supplied left-side LCD on screen left. Preserve outward winding.
					var reflected = quads.stream().map(q -> new BakedQuad(
						q.position3(),q.position2(),q.position1(),q.position0(),
						q.packedUV3(),q.packedUV2(),q.packedUV1(),q.packedUV0(),q.direction(),q.materialInfo())).toList();
					collector.submitItem(pose,context,light,overlay,outline,tints,reflected,foil);
				}
			}, 0xF000F0);
	}
	/** Screen rectangle inscribed in a face on the plane x = const (model units, 0..16 per block). */
	public static Rect surface(double width, double height, float x, float y0, float y1, float z0, float z1) {
		return project(width,height,new float[][]{{x,y1,z0},{x,y1,z1},{x,y0,z0},{x,y0,z1}});
	}
	/** The same for a face on the plane z = const, like the left side panel. */
	public static Rect surfaceZ(double width, double height, float z, float x0, float x1, float y0, float y1) {
		return project(width,height,new float[][]{{x0,y1,z},{x1,y1,z},{x0,y0,z},{x1,y0,z}});
	}
	// Corners in order: top pair, then bottom pair.
	public static Rect project(double width, double height, float[][] corners) {
		Matrix4f p = new Matrix4f(projection);
		// Preserve the exact physical-window projection for the current GUI (scaled sizes round up).
		// Other dimensions are used for layout previews and resize checks.
		if (width != guiWidth || height != guiHeight) {
			p.m00(p.m11()/(float)(width/height));
		}
		p.mul(pose(width,height)).translate(-.5f,-.5f,-.5f);
		Vector3f[] v = new Vector3f[4];
		for (int i=0;i<4;i++) {
			Vector3f q=p.transformProject(new Vector3f(corners[i][0]/16,corners[i][1]/16,corners[i][2]/16));
			v[i]=new Vector3f((q.x+1)*(float)width/2,(1-q.y)*(float)height/2,0);
		}
		// Inscribed rectangle keeps HUD and processed pixels inside a slightly tilted quad.
		double left=Math.max(Math.min(v[0].x,v[1].x),Math.min(v[2].x,v[3].x));
		double right=Math.min(Math.max(v[0].x,v[1].x),Math.max(v[2].x,v[3].x));
		double top=Math.max(v[0].y,v[1].y), bottom=Math.min(v[2].y,v[3].y);
		return new Rect(left,top,right-left,bottom-top);
	}
}
