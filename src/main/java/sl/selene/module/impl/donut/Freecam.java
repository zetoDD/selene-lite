package sl.selene.module.impl.donut;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import sl.selene.event.EventInit;
import sl.selene.event.input.MouseScrollEvent;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.event.player.EventInput;
import sl.selene.event.player.EventLook;
import sl.selene.event.player.EventRotation;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;

@IModule(
   name = "Freecam",
   description = "Fly a detached camera while your body stays frozen in place",
   category = Category.Donut,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Freecam extends Module {
   public static Freecam instance;

   public static final SliderSetting travelSpeed = new SliderSetting("Travel Speed", 1.5F, 0.1F, 10.0F, 0.1F, false);
   public static final SliderSetting turboBoost = new SliderSetting("Turbo Boost", 2.0F, 1.0F, 8.0F, 0.5F, false);
   public static final BooleanSetting smoothMotion = new BooleanSetting("Smooth Motion", true);
   public static final BooleanSetting ghostCrouch = new BooleanSetting("Ghost Crouch", false);
   public static final BooleanSetting echoInput = new BooleanSetting("Echo Input", false);
   public static final BooleanSetting detachedView = new BooleanSetting("Detached View", true);
   public static final BooleanSetting revealEntities = new BooleanSetting("Reveal Entities", true);
   public static final BooleanSetting scrollSteer = new BooleanSetting("Scroll Steer", true);
   public static final SliderSetting viewRange = new SliderSetting("View Range", 16.0F, 4.0F, 32.0F, 1.0F, false);

   private static final double SMOOTH_BASE = 0.001;
   private static final float LOOK_SENSITIVITY = 0.5F;
   private static final float MAX_FRAME_DELTA = 0.1F;
   private static final float MIN_FRAME_DELTA = 0.016F;
   private static final double MAX_RENDER_DISTANCE = 25600.0;

   private final Vector3d currentPosition = new Vector3d();
   private final Vector3d previousPosition = new Vector3d();
   private final Vector3d velocity = new Vector3d();
   private float yaw;
   private float pitch;
   private float previousYaw;
   private float previousPitch;
   private float currentSpeed;
   private long lastFrameTime;
   private Perspective savedPerspective;
   private boolean savedChunkCullingEnabled;
   private float savedPlayerYaw;
   private float savedPlayerPitch;
   private boolean wasSneaking;
   private boolean heldSneak;
   private boolean heldSprint;
   private int savedViewDistance;

   public Freecam() {
      instance = this;
      this.addSettings(new Setting[] { travelSpeed, turboBoost, smoothMotion, ghostCrouch, echoInput, detachedView, revealEntities, scrollSteer, viewRange });
   }

   @Override
   public String getArrayListSuffix() {
      return fmt(travelSpeed.get());
   }

   @Override
   public void onEnable() {
      super.onEnable();
      instance = this;
      if (mc.player == null || mc.world == null) {
         this.toggle();
         return;
      }

      this.savedPerspective = mc.options.getPerspective();
      this.savedChunkCullingEnabled = mc.chunkCullingEnabled;
      mc.chunkCullingEnabled = false;
      this.expandLoadedWorld();
      this.savedPlayerYaw = mc.player.getYaw();
      this.savedPlayerPitch = mc.player.getPitch();
      this.wasSneaking = mc.player.isSneaking();
      this.captureHeldInput();
      this.yaw = mc.player.getYaw();
      this.pitch = mc.player.getPitch();
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
      Vec3d camPos = mc.player.getCameraPosVec(1.0F);
      this.currentPosition.set(camPos.x, camPos.y, camPos.z);
      this.previousPosition.set(camPos.x, camPos.y, camPos.z);
      this.velocity.set(0.0, 0.0, 0.0);
      this.currentSpeed = this.getBaseSpeed();
      this.lastFrameTime = System.currentTimeMillis();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (mc.player != null) {
         mc.player.setYaw(this.savedPlayerYaw);
         mc.player.setPitch(this.savedPlayerPitch);
         mc.player.setHeadYaw(this.savedPlayerYaw);
         mc.player.setBodyYaw(this.savedPlayerYaw);
         mc.player.setSneaking(this.wasSneaking);
         mc.player.setSprinting(false);
      }

      if (this.savedPerspective != null) {
         mc.options.setPerspective(this.savedPerspective);
      } else {
         mc.options.setPerspective(Perspective.FIRST_PERSON);
      }

      mc.chunkCullingEnabled = this.savedChunkCullingEnabled;
      this.restoreLoadedWorld();
      this.velocity.set(0.0, 0.0, 0.0);
      this.clearHeldInput();
   }

   private void expandLoadedWorld() {
      if (mc.world == null || mc.options == null) {
         return;
      }

      this.savedViewDistance = mc.options.getViewDistance().getValue();
      int target = MathHelper.clamp(Math.round(viewRange.get()), 2, 32);
      if (target > this.savedViewDistance) {
         mc.options.getViewDistance().setValue(target);
         mc.world.getChunkManager().updateLoadDistance(target);
      }
   }

   private void restoreLoadedWorld() {
      if (mc.options == null) {
         return;
      }

      mc.options.getViewDistance().setValue(this.savedViewDistance);
      if (mc.world != null) {
         mc.world.getChunkManager().updateLoadDistance(this.savedViewDistance);
      }
   }

   private void captureHeldInput() {
      boolean sneaking = mc.player != null && mc.player.isSneaking();
      this.heldSneak = mc.options != null && (mc.options.sneakKey.isPressed() || sneaking);
      this.heldSprint = mc.options != null && (mc.options.sprintKey.isPressed() || mc.player.isSprinting());
   }

   private void clearHeldInput() {
      this.heldSneak = false;
      this.heldSprint = false;
   }

   @EventInit
   public void onClientTick(ClientTickEvent event) {
      if (mc.player == null) {
         return;
      }

      mc.player.setYaw(this.savedPlayerYaw);
      mc.player.setPitch(this.savedPlayerPitch);
      mc.player.setHeadYaw(this.savedPlayerYaw);
      mc.player.setBodyYaw(this.savedPlayerYaw);
      if (echoInput.get()) {
         mc.player.setSneaking(this.heldSneak);
         mc.player.setSprinting(this.heldSprint);
      } else if (ghostCrouch.get() || this.wasSneaking) {
         mc.player.setSneaking(ghostCrouch.get() || this.wasSneaking);
      }
   }

   @EventInit
   public void onLook(EventLook event) {
      this.updateRotation((float) (event.getYaw() * 0.15F * LOOK_SENSITIVITY), (float) (event.getPitch() * 0.15F * LOOK_SENSITIVITY));
      event.cancel();
   }

   @EventInit
   public void onRotation(EventRotation event) {
      event.setYaw(this.getInterpolatedYaw(event.getPartialTicks()));
      event.setPitch(this.getInterpolatedPitch(event.getPartialTicks()));
   }

   @EventInit
   public void onInput(EventInput event) {
      event.setForward(0.0F);
      event.setStrafe(0.0F);
      event.setJump(false);
      event.setSneak(false);
   }

   @EventInit
   public void onScroll(MouseScrollEvent event) {
      if (scrollSteer.get() && mc.currentScreen == null) {
         double delta = event.vertical() != 0.0 ? event.vertical() : event.horizontal();
         if (delta != 0.0) {
            this.adjustSpeed(delta);
            event.cancel();
         }
      }
   }

   public void tickCameraView(float tickProgress) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      float moveYaw = this.previousYaw + MathHelper.wrapDegrees(this.yaw - this.previousYaw) * tickProgress;
      double yawRad = Math.toRadians(moveYaw);
      double sinYaw = -Math.sin(yawRad);
      double cosYaw = Math.cos(yawRad);
      double rightX = cosYaw;
      double rightZ = -sinYaw;

      this.previousPosition.set(this.currentPosition);
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
      long now = System.currentTimeMillis();
      float delta = (float) (now - this.lastFrameTime) / 1000.0F;
      this.lastFrameTime = now;
      delta = Math.min(delta, MAX_FRAME_DELTA);
      if (delta < MIN_FRAME_DELTA) {
         delta = MIN_FRAME_DELTA;
      }

      double moveX = 0.0;
      double moveY = 0.0;
      double moveZ = 0.0;
      double step = this.currentSpeed * 2.0;
      if (mc.options != null && mc.options.sprintKey.isPressed()) {
         step *= Math.max(1.0F, turboBoost.get());
      }

      boolean frontView = mc.options != null
            && mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT;

      if (mc.options != null) {
         if (mc.options.forwardKey.isPressed()) {
            moveX += sinYaw * step;
            moveZ += cosYaw * step;
         }
         if (mc.options.backKey.isPressed()) {
            moveX -= sinYaw * step;
            moveZ -= cosYaw * step;
         }
         if (mc.options.rightKey.isPressed()) {
            if (frontView) {
               moveX -= rightX * step;
               moveZ -= rightZ * step;
            } else {
               moveX += rightX * step;
               moveZ += rightZ * step;
            }
         }
         if (mc.options.leftKey.isPressed()) {
            if (frontView) {
               moveX += rightX * step;
               moveZ += rightZ * step;
            } else {
               moveX -= rightX * step;
               moveZ -= rightZ * step;
            }
         }
         if (mc.options.jumpKey.isPressed()) {
            moveY += step;
         }
         if (mc.options.sneakKey.isPressed()) {
            moveY -= step;
         }
      }

      if (smoothMotion.get()) {
         double blend = 1.0 - Math.pow(SMOOTH_BASE, delta);
         this.velocity.x = MathHelper.lerp(blend, this.velocity.x, moveX * 5.0);
         this.velocity.y = MathHelper.lerp(blend, this.velocity.y, moveY * 5.0);
         this.velocity.z = MathHelper.lerp(blend, this.velocity.z, moveZ * 5.0);
      } else {
         this.velocity.set(moveX * 5.0, moveY * 5.0, moveZ * 5.0);
      }

      this.currentPosition.x += this.velocity.x * delta;
      this.currentPosition.y += this.velocity.y * delta;
      this.currentPosition.z += this.velocity.z * delta;
   }

   private void updateRotation(float deltaYaw, float deltaPitch) {
      this.yaw += deltaYaw;
      this.pitch += deltaPitch;
      this.yaw = MathHelper.wrapDegrees(this.yaw);
      this.pitch = MathHelper.clamp(this.pitch, -90.0F, 90.0F);
   }

   private void adjustSpeed(double scrollDelta) {
      float next = this.currentSpeed + (float) Math.signum(scrollDelta) * 0.2F;
      this.currentSpeed = MathHelper.clamp(next, travelSpeed.minimum, travelSpeed.maximum);
   }

   private float getBaseSpeed() {
      return MathHelper.clamp(travelSpeed.get(), travelSpeed.minimum, travelSpeed.maximum);
   }

   public double getInterpolatedX(float partialTicks) {
      return MathHelper.lerp(partialTicks, this.previousPosition.x, this.currentPosition.x);
   }

   public double getInterpolatedY(float partialTicks) {
      return MathHelper.lerp(partialTicks, this.previousPosition.y, this.currentPosition.y);
   }

   public double getInterpolatedZ(float partialTicks) {
      return MathHelper.lerp(partialTicks, this.previousPosition.z, this.currentPosition.z);
   }

   public float getInterpolatedYaw(float partialTicks) {
      return this.previousYaw + MathHelper.wrapDegrees(this.yaw - this.previousYaw) * partialTicks;
   }

   public float getInterpolatedPitch(float partialTicks) {
      return MathHelper.lerp(partialTicks, this.previousPitch, this.pitch);
   }

   public boolean shouldRenderSneaking() {
      return echoInput.get() ? this.heldSneak : ghostCrouch.get() || this.wasSneaking;
   }

   public boolean shouldKeepThirdPerson() {
      return detachedView.get();
   }

   public boolean shouldRevealEntities() {
      return revealEntities.get();
   }

   public static boolean isActive() {
      return instance != null && instance.enable;
   }

   public static double getMaxRenderDistance() {
      return MAX_RENDER_DISTANCE;
   }
}