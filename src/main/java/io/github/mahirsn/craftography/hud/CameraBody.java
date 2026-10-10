package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.hud.BodyControls.Control;
import io.github.mahirsn.craftography.hud.BodyControls.Page;
import io.github.mahirsn.craftography.hud.BodyLayout.Part;
import io.github.mahirsn.craftography.render.Meter;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** LCD readouts and animated hardware over the projected held model. */
public final class CameraBody {
	static final int AMBER = 0xD8A261;
	private static final int PRINT = 0xD6DAD8;
	private CameraBody() { }

	private static void engraving(GuiGraphicsExtractor g, BodyLayout l, String text, double cx, double y, int color) {
		Font font = Minecraft.getInstance().font;
		float size = (float)(l.scale * .58);
		g.pose().pushMatrix();
		g.pose().translate((float)cx, (float)y);
		g.pose().scale(size, size);
		g.text(font,text,-font.width(text)/2,0,color,false);
		g.pose().popMatrix();
	}
	public static void draw(GuiGraphicsExtractor g, BodyLayout l, Part hovered) {
		Font font = Minecraft.getInstance().font;
		CameraSettings s = CameraSettings.get();

		wheel(g, l.get(Part.ZOOM), Part.ZOOM, hovered == Part.ZOOM, 1, l.scale);
		Rect zoom=l.get(Part.ZOOM);
		engraving(g,l,"W / T",zoom.x()+zoom.w()/2,zoom.y()+zoom.h()/2-2*l.scale,Draw.argb(.65f,PRINT));
		Rect release = l.get(Part.SHUTTER);
		button(g, release, Part.SHUTTER, hovered == Part.SHUTTER, false, 1, l.scale);
		// A recessed segmented selector. Its narrow highlight slides between modes.
		double mode = HardwareMotion.pagePosition();
		Rect active = l.pageButton(0);
		Draw.rect(g, active.x()+mode*active.w()+3, active.bottom()-2, active.x()+mode*active.w()+active.w()-3, active.bottom()-1, 0xFFCBD0D6);
		for (Page page : Page.values()) {
			Rect r = l.pageButton(page.ordinal());
			String name = page.label;
			engraving(g,l,name,r.x()+r.w()/2,r.y()+1*l.scale,Draw.argb(page == BodyControls.page() ? .95f : .45f, PRINT));
		}
		float fade = BodyControls.pageFade();
		if (fade < 1) for (Part part : BodyControls.previousPage().parts) hardware(g,l,BodyControls.get(part),false,1-fade);
		for (Part part : BodyControls.page().parts) hardware(g,l,BodyControls.get(part),part == hovered,fade);
		if (Shutter.timerRunning()) {
			float pulse=(float)(.35+.65*(.5+.5*Math.sin(net.minecraft.util.Util.getMillis()/120.0)));
			Draw.rect(g,release.right()+4,release.y()+6,release.right()+6,release.y()+8,Draw.argb(pulse,AMBER));
		}
		double uiScale = ViewfinderHud.lcdScale(l.lcd);
		g.pose().pushMatrix();
		g.pose().translate((float)l.lcd.x(), (float)l.lcd.y());
		g.pose().scale((float)uiScale, (float)uiScale);
		Rect lcd = new Rect(0,0,l.lcd.w()/uiScale,l.lcd.h()/uiScale);

		String info = Math.round(s.focalLength)+"mm  "+ExposureMath.formatAperture(s.aperture)+"  "+ExposureMath.formatShutter(s.shutter)+"  ISO"+s.iso;
		float infoScale=(float)Math.min(1,(lcd.w()-16)/font.width(info));
		g.pose().pushMatrix();
		g.pose().translate((float)lcd.x()+8,(float)lcd.bottom()-16);
		g.pose().scale(infoScale,infoScale);
		g.text(font,info,0,0,Draw.argb(.9f,PRINT),true);
		g.pose().popMatrix();
		String state = Shutter.timerRunning() ? "TIMER "+Shutter.timerSeconds()+"s" : "STBY" + (s.selfTimer > 0 ? "  " + s.selfTimer + "s" : "");
		g.text(font,state,(int)lcd.x()+8,(int)lcd.y()+16,Draw.argb(1f,Shutter.timerRunning()?AMBER:PRINT),true);
		String pageName = BodyControls.page().label;
		g.text(font,pageName,(int)lcd.right()-font.width(pageName)-8,(int)lcd.y()+16,Draw.argb(fade,PRINT),true);
		ruler(g,lcd);
		g.pose().popMatrix();
	}

	private static String label(Part p) {
		return switch(p) {
			case ISO -> "ISO"; case APERTURE -> "IRIS"; case SHUTTER_SPEED -> "SPEED";
			case NOISE -> "GRAIN"; case HISTOGRAM -> "HIST"; case KELVIN -> "WB"; case TINT -> "TINT";
			case FOCUS -> "FOCUS"; case FOCUS_MODE -> "AF/MF"; case PEAKING -> "PEAK";
			case ASPECT -> "ASPECT"; case GUIDE -> "GUIDE"; case LEVEL -> "LEVEL"; case MASK -> "MASK"; case GRID -> "GRID";
			case TIME -> "CLOCK"; case WEATHER -> "SKY"; case MOON -> "MOON"; case SERVER_TIME -> "RESET"; case LOCK -> "LOCK";
			case TIMER -> "TIMER"; case TRIPOD -> "TRIPOD"; case PREVIEW -> "PLAY";
			default -> "";
		};
	}
	private static void wheel(GuiGraphicsExtractor g, Rect r, Part p, boolean hover, float alpha, double scale) {
		// Keep circular dials circular inside the projected hardware button quad.
		double side = Math.min(r.w(), r.h());
		r = new Rect(r.x()+(r.w()-side)/2, r.y()+(r.h()-side)/2, side, side);
		String size=r.h()/scale<40?"_small":"";
		Draw.camera(g,"body/control_dial"+size+(hover?"_hl":""),r.x(),r.y(),(int)r.w(),(int)r.h(),Draw.argb(alpha,0xFFFFFF));
		g.pose().pushMatrix();
		g.pose().translate((float)(r.x()+r.w()/2),(float)(r.y()+r.h()/2));
		g.pose().rotate((float)(HardwareMotion.position(p)*Math.PI/8));
		Draw.camera(g,"body/control_marks"+size,-r.w()/2,-r.h()/2,(int)r.w(),(int)r.h(),Draw.argb(alpha,0xFFFFFF));
		g.pose().popMatrix();
	}
	private static void button(GuiGraphicsExtractor g, Rect r, Part p, boolean hover, boolean lit, float alpha, double scale) {
		// Pressed: the button sinks one texel instead of shrinking, so its pixel art keeps its shape.
		double inset=HardwareMotion.inset(p)*scale;
		Draw.camera(g,"body/"+(p==Part.SHUTTER?"capture_cap":r.h()/scale>=40?"round_button_big":r.h()/scale>=28?"round_button":"button")+(lit?"_on":"")+(hover?"_hl":""),r.x(),r.y()+inset,(int)r.w(),(int)r.h(),Draw.argb(alpha,inset>.2?0xC0C0C0:0xFFFFFF));
	}
	private static void hardware(GuiGraphicsExtractor g, BodyLayout l, Control c, boolean hover, float alpha) {
		Rect r=l.get(c.part());
		Font font=Minecraft.getInstance().font;
		if(c.step()!=null) wheel(g,r,c.part(),hover,alpha,l.scale);
		else button(g,r,c.part(),hover,c.lit().getAsBoolean(),alpha,l.scale);
		String icon=c.icon()==null?switch(c.part()) {
			case ISO -> "iso"; case APERTURE -> "iris"; case SHUTTER_SPEED -> "speed";
			case KELVIN -> "kelvin"; case TINT -> "tint"; case FOCUS,FOCUS_MODE -> "focus"; case TIME -> "clock";
			default -> "frame";
		}:c.icon().get();
		Draw.camera(g,"body/icon_"+icon,r.x()+r.w()/2-5,r.y()+r.h()/2-4+HardwareMotion.inset(c.part()),11,9,Draw.argb(alpha*.8f,PRINT));
		String name=label(c.part());
		if (r.y() < l.y + 215*l.scale) engraving(g,l,name,r.x()+r.w()/2,r.bottom()+1*l.scale,Draw.argb(alpha*.6f,PRINT));
		if(hover&&c.value()!=null) {
			double uiScale = ViewfinderHud.lcdScale(l.lcd);
			g.pose().pushMatrix();
			g.pose().translate((float)l.lcd.x(), (float)l.lcd.y());
			g.pose().scale((float)uiScale, (float)uiScale);
			String readout=font.plainSubstrByWidth(label(c.part())+"  "+c.value().get().getString(),(int)(l.lcd.w()/uiScale)-24);
			Draw.rect(g,5,29,11+font.width(readout),43,0xB0101318);
			g.text(font,readout,8,32,Draw.argb(alpha,PRINT),false);
			g.pose().popMatrix();
		}
	}
	/** Scales move beneath a fixed needle; labels fade naturally at the ends. */
	private static void ruler(GuiGraphicsExtractor g, Rect lcd) {
		Page page=BodyControls.page();
		Part p=page==Page.COLOUR?Part.KELVIN:page==Page.FOCUS?Part.FOCUS:null;
		boolean ev=page==Page.EXPOSURE;
		if(p==null&&!ev)return;
		Font font=Minecraft.getInstance().font;
		double cx=lcd.x()+lcd.w()/2, yy=lcd.bottom()-30;
		double radius=Math.min(75,lcd.w()/2-16);
		double value=ev?HardwareMotion.exposurePosition(Double.isFinite(Meter.level())?Meter.level():0):HardwareMotion.position(p);
		int base=(int)Math.floor(value);
		double spacing=ev?10:8;
		for(int i=base-10;i<=base+10;i++) {
			double xx=cx+(i-value)*spacing;
			double a=Math.max(0,1-Math.abs(xx-cx)/radius)*.55*BodyControls.pageFade();
			if(a<=0)continue;
			boolean major=i% (ev?4:5)==0;
			Draw.line(g,xx,yy,xx,yy+(major?4:2),Draw.argb((float)a,PRINT));
			if(major) {
				String text=ev?Integer.toString(i/4):p==Part.KELVIN?Integer.toString(i*100):ExposureMath.formatFocus(Math.pow(1.2,i));
				if(ev&&Math.abs(i)>12)continue;
				g.text(font,text,(int)(xx-font.width(text)/2),(int)yy-10,Draw.argb((float)a,PRINT),false);
			}
		}
		Draw.line(g,cx,yy-2,cx,yy+5,Draw.argb(.8f*BodyControls.pageFade(),PRINT));
	}
}
