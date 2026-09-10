package sl.selene.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.client.ClientLookState;
import sl.selene.event.EventManager;
import sl.selene.event.impl.EventUpdate;
import sl.selene.event.player.EventMotion;
import sl.selene.event.player.SlowWalkingEvent;
import sl.selene.event.player.UsingItemEvent;
import sl.selene.util.player.RotationUtil;

@Environment(EnvType.CLIENT)
@Mixin({ ClientPlayerEntity.class })
public abstract class ClientPlayerEntityMixin {
   @Shadow
   protected Input input;

   @Unique
   private float selene$silentPrevYaw;
   @Unique
   private float selene$silentPrevPitch;
   @Unique
   private boolean selene$silentSwapped;

   @Inject(method = { "tick" }, at = { @At("HEAD") })
   private void onTickHead(CallbackInfo ci) {
      EventManager.call(new EventUpdate());
   }

   @Inject(method = "sendMovementPackets", at = @At("HEAD"))
   private void selene$applySilentRotation(CallbackInfo ci) {
      ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
      if (RotationUtil.hasSilentRotation() && MinecraftClient.getInstance().player == self) {
         this.selene$silentPrevYaw = self.getYaw();
         this.selene$silentPrevPitch = self.getPitch();
         self.setYaw(RotationUtil.getServerYaw());
         self.setPitch(RotationUtil.getServerPitch());
         this.selene$silentSwapped = true;
      }
   }

   @Inject(method = "sendMovementPackets", at = @At("RETURN"))
   private void selene$restoreRotation(CallbackInfo ci) {
      if (this.selene$silentSwapped) {
         this.selene$silentSwapped = false;
         ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
         self.setYaw(this.selene$silentPrevYaw);
         self.setPitch(this.selene$silentPrevPitch);
      }
   }

   @Redirect(method = {
         "tickMovementInput" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
   private float redirectRenderPitchUpdate(ClientPlayerEntity instance) {
      return ClientLookState.active ? MinecraftClient.getInstance().gameRenderer.getCamera().getPitch()
            : instance.getPitch();
   }

   @ModifyExpressionValue(method = { "applyMovementSpeedFactors" }, at = {
         @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z") })
   private boolean usingItemHook(boolean original) {
      if (original) {
         UsingItemEvent event = new UsingItemEvent((byte) 1);
         EventManager.call(event);
         if (event.isCancelled()) {
            return false;
         }
      }

      return original;
   }

   @Redirect(method = {
         "tickMovementInput" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
   private float redirectRenderYawUpdate(ClientPlayerEntity instance) {
      return ClientLookState.active ? MinecraftClient.getInstance().gameRenderer.getCamera().getYaw() : instance.getYaw();
   }

   @Inject(method = { "tickMovement" }, at = { @At("HEAD") })
   private void onUpdateWalkingPlayer(CallbackInfo ci) {
      ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
      if (player != null) {
         Box axisalignedbb = player.getBoundingBox();
         EventMotion eventPre = new EventMotion(
               player.getYaw(),
               player.getPitch(),
               player.getX(),
               player.getY(),
               player.getZ(),
               player.isOnGround(),
               axisalignedbb,
               null);
         EventManager.call(eventPre);
         if (!eventPre.isCancelled()) {
            if (eventPre.getYaw() != player.getYaw() || eventPre.getPitch() != player.getPitch()) {
               player.setYaw(eventPre.getYaw());
               player.setPitch(eventPre.getPitch());
            }

            if (eventPre.getPosX() != player.getX() || eventPre.getPosY() != player.getY()
                  || eventPre.getPosZ() != player.getZ()) {
               player.refreshPositionAndAngles(eventPre.getPosX(), eventPre.getPosY(), eventPre.getPosZ(),
                     eventPre.getYaw(), eventPre.getPitch());
            }

            if (eventPre.isOnGround() != player.isOnGround()) {
               player.setOnGround(eventPre.isOnGround());
            }
         }
      }
   }

   @Redirect(method = {
         "applyMovementSpeedFactors" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec2f;multiply(F)Lnet/minecraft/util/math/Vec2f;", ordinal = 1))
   private Vec2f preventSlowdownMultiply(Vec2f vec, float multiplier) {
      ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
      if (multiplier == 0.2F && player.isUsingItem() && !player.hasVehicle()) {
         float moveForward = vec.y;
         float moveStrafe = vec.x;
         SlowWalkingEvent event = new SlowWalkingEvent(moveForward, moveStrafe);
         EventManager.call(event);
         event.hook();
         if (event.isCancelled()) {
            return vec;
         }
      }

      return vec.multiply(multiplier);
   }

   @Unique
   private static float getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }
}