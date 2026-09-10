package sl.selene.util.render.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public class ScrollUtil {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    private static ScaledResolution sr;
    private static Window mw;
    private float rawTarget;
    private float target;
    private float scroll;
    private float velocity;
    private float max;
    private float speed = 8.0F;
    private boolean enabled;
    float barHeight;
    private float trackX;
    private float trackY;
    private float trackWidth;
    private float trackHeight;
    private boolean scrollbarDragging;
    private float scrollbarDragOffsetY;

    private static final float SPRING_STIFFNESS = 200.0F;
    private static final float SPRING_DAMPING = 28.0F;
    private static final float DT = 1.0F / 60.0F;
    private static final float RUBBER_DIM = 48.0F;
    private static final float RUBBER_CONSTANT = 0.55F;
    private static final float RUBBER_RETURN = 0.18F;

    public static ScaledResolution getScaledResolution() {
        if (sr == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                sr = new ScaledResolution(client);
            }
        }

        return sr;
    }

    public static Window getWindow() {
        if (mw == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                mw = client.getWindow();
            }
        }

        return mw;
    }

    public ScrollUtil() {
        this.setEnabled(true);
    }

    public void update() {
        if (this.max >= 0.0F) {
            this.rawTarget = 0.0F;
            this.target = 0.0F;
        } else {
            if (this.rawTarget > 0.0F) {
                this.rawTarget = releaseToward(this.rawTarget, 0.0F);
            } else if (this.rawTarget < this.max) {
                this.rawTarget = releaseToward(this.rawTarget, this.max);
            }

            this.target = rubberbandClamp(this.rawTarget);
        }

        float accel = SPRING_STIFFNESS * (this.target - this.scroll) - SPRING_DAMPING * this.velocity;
        this.velocity += accel * DT;
        this.scroll += this.velocity * DT;
        if (Math.abs(this.target - this.scroll) < 0.01F && Math.abs(this.velocity) < 0.01F) {
            this.scroll = this.target;
            this.velocity = 0.0F;
        }
    }

    private static float releaseToward(float value, float limit) {
        float next = limit + (value - limit) * (1.0F - RUBBER_RETURN);
        return Math.abs(next - limit) < 0.25F ? limit : next;
    }

    private float rubberbandClamp(float raw) {
        if (raw > 0.0F) {
            return rubberband(raw);
        } else {
            return raw < this.max ? this.max - rubberband(this.max - raw) : raw;
        }
    }

    private static float rubberband(float overshoot) {
        return overshoot * RUBBER_DIM * RUBBER_CONSTANT / (RUBBER_DIM + RUBBER_CONSTANT * overshoot);
    }

    public void handleScroll(double scrollY) {
        if (this.enabled) {
            if (this.max >= 0.0F) {
                this.rawTarget = 0.0F;
                this.target = 0.0F;
                return;
            }

            float wheel = (float) scrollY * (this.speed * 5.0F);
            this.rawTarget += wheel;
            this.target = rubberbandClamp(this.rawTarget);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Number> T lerp(T input, T target, double step) {
        double start = input.doubleValue();
        double end = target.doubleValue();
        double result = start + step * (end - start);
        if (input instanceof Integer) {
            return (T) Integer.valueOf((int) Math.round(result));
        } else if (input instanceof Double) {
            return (T) Double.valueOf(result);
        } else if (input instanceof Float) {
            return (T) Float.valueOf((float) result);
        } else if (input instanceof Long) {
            return (T) Long.valueOf(Math.round(result));
        } else if (input instanceof Short) {
            return (T) Short.valueOf((short) Math.round(result));
        } else if (input instanceof Byte) {
            return (T) Byte.valueOf((byte) Math.round(result));
        } else {
            throw new IllegalArgumentException("Unsupported type: " + input.getClass().getSimpleName());
        }
    }

    public static void enable() {
        GL11.glEnable(3089);
    }

    public static void disable() {
        GL11.glDisable(3089);
    }

    public static void scissor(Window window, double x, double y, double width, double height) {
        if (x + width != x && y + height != y && !(x < 0.0) && !(y + height < 0.0)) {
            double scaleFactor = window.getScaleFactor();
            GL11.glScissor(
                (int) Math.round(x * scaleFactor),
                (int) Math.round((window.getScaledHeight() - (y + height)) * scaleFactor),
                (int) Math.round(width * scaleFactor),
                (int) Math.round(height * scaleFactor));
        }
    }

    public void reset() {
        this.scroll = 0.0F;
        this.target = 0.0F;
        this.rawTarget = 0.0F;
        this.velocity = 0.0F;
        this.stopScrollbarDrag();
    }

    public void stopScrollbarDrag() {
        this.scrollbarDragging = false;
        this.scrollbarDragOffsetY = 0.0F;
    }

    public boolean isScrollbarDragging() {
        return this.scrollbarDragging;
    }

    public boolean hasScrollbar() {
        return this.getMax() < 0.0F;
    }

    public boolean handleScrollbarClick(float mouseX, float mouseY, int button) {
        if (button != 0 || !this.enabled || !this.hasScrollbar()) {
            return false;
        }

        float[] thumb = this.getThumbBounds();
        if (thumb == null) {
            return false;
        }

        if (this.isHovered(mouseX, mouseY, thumb[0], thumb[1], thumb[2], thumb[3])) {
            this.scrollbarDragging = true;
            this.scrollbarDragOffsetY = mouseY - thumb[1];
            this.target = this.scroll;
            this.rawTarget = this.scroll;
            this.velocity = 0.0F;
            return true;
        }

        if (this.isHovered(mouseX, mouseY, this.trackX, this.trackY, this.trackWidth, this.trackHeight)) {
            this.jumpScrollToMouse(mouseY, thumb[3]);
            this.scrollbarDragging = true;
            this.scrollbarDragOffsetY = thumb[3] * 0.5F;
            return true;
        }

        return false;
    }

    public boolean handleScrollbarDrag(float mouseY) {
        if (!this.scrollbarDragging || !this.hasScrollbar()) {
            return false;
        }

        float[] thumb = this.getThumbBounds();
        if (thumb == null) {
            return false;
        }

        float thumbHeight = thumb[3];
        float travel = this.trackHeight - thumbHeight;
        if (travel <= 0.0F) {
            return true;
        }

        float thumbY = Math.max(this.trackY, Math.min(mouseY - this.scrollbarDragOffsetY, this.trackY + travel));
        float ratio = (thumbY - this.trackY) / travel;
        this.target = this.getMax() * ratio;
        this.scroll = this.target;
        this.rawTarget = this.target;
        this.velocity = 0.0F;
        return true;
    }

    private void jumpScrollToMouse(float mouseY, float thumbHeight) {
        float travel = this.trackHeight - thumbHeight;
        if (travel <= 0.0F) {
            return;
        }

        float ratio = Math.max(0.0F, Math.min(1.0F, (mouseY - this.trackY - thumbHeight * 0.5F) / travel));
        this.target = this.getMax() * ratio;
        this.scroll = this.target;
        this.rawTarget = this.target;
        this.velocity = 0.0F;
    }

    private float[] getThumbBounds() {
        if (!this.hasScrollbar() || this.trackHeight <= 0.0F) {
            return null;
        }

        float thumbHeight = this.trackHeight - this.getMax() / (this.getMax() - this.trackHeight) * this.trackHeight;
        if (this.barHeight > 0.0F && this.barHeight < this.trackHeight) {
            thumbHeight = this.barHeight;
        }
        if (thumbHeight <= 0.0F || thumbHeight >= this.trackHeight) {
            return null;
        }

        float percentage = this.getMax() != 0.0F ? this.getScroll() / this.getMax() : 0.0F;
        float thumbY = this.trackY + (this.trackHeight - thumbHeight) * percentage;
        return new float[] { this.trackX, thumbY, this.trackWidth, thumbHeight };
    }

    private static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY <= y + height && mouseX <= x + width && mouseY >= y;
    }

    public void setMax(float max, float height) {
        this.max = -max + height;
    }

    public void render(Renderer2D renderer2D, float x, float y, float width, float height, float alpha) {
        this.trackX = x;
        this.trackY = y;
        this.trackWidth = width;
        this.trackHeight = height;
        if (!(this.getMax() >= 0.0F)) {
            float percentage = this.getMax() != 0.0F ? this.getScroll() / this.getMax() : 0.0F;
            percentage = MathHelper.clamp(percentage, 0.0F, 1.0F);
            float targetBarHeight = height - this.getMax() / (this.getMax() - height) * height;
            this.barHeight = MathHelper.interpolate(targetBarHeight, this.barHeight, 0.9F);
            boolean allowed = this.barHeight < height && this.barHeight > 0.0F;
            if (allowed) {
                float scrollY = y + height * percentage - this.barHeight * percentage;
                int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1),
                    (int) MathHelper.clamp(255.0F * alpha, 0.0F, 255.0F));
                int mainColor20 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1),
                    (int) MathHelper.clamp(20.0F * alpha, 0.0F, 20.0F));
                renderer2D.rect(x, y, width, height, mainColor20);
                renderer2D.rect(x, scrollY, width, this.barHeight, 1.0F, mainColor);
            }
        }
    }

    public float getTarget() {
        return this.target;
    }

    public void setTarget(float target) {
        this.target = target;
        this.rawTarget = target;
    }

    public float getScroll() {
        return this.scroll;
    }

    public void setScroll(float scroll) {
        this.scroll = scroll;
        this.velocity = 0.0F;
    }

    public float getMax() {
        return this.max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}