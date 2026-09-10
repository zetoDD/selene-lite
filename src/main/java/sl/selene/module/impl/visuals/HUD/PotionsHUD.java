package sl.selene.module.impl.visuals.HUD;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import sl.selene.module.impl.visuals.HUD.HudEditor;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.ScaledResolution;
import sl.selene.util.render.math.animation.AnimationMath;
import sl.selene.util.render.math.animation.anim.util.Animation2;
import sl.selene.util.render.math.animation.anim.util.Easings;
import sl.selene.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class PotionsHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();

   private static final float ROW_H = 30.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float PAD_X = 7.0F;
   private static final float TEXT_PAD = 8.0F;
   private static final float NAME_SIZE = 14.0F;
   private static final float DUR_SIZE = 11.0F;
   private static final float BAR_H = 2.5F;
   private static final float BAR_BOTTOM_OFFSET = 5.0F;
   private static final int HARMFUL_TEXT = new Color(16734547).getRGB();

   private static final Map<RegistryEntry<StatusEffect>, Integer> maxDurations = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Float> animatedWidths = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Float> animatedY = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, Animation2> animatedAlphas = new HashMap<>();
   private static final Map<RegistryEntry<StatusEffect>, StatusEffectInstance> cachedEffects = new HashMap<>();

   public static void potions(Renderer2D r2) {
      new ScaledResolution(mc);
      if (mc.player == null) {
         return;
      }
      Set<RegistryEntry<StatusEffect>> activeEffects = mc.player
            .getStatusEffects()
            .stream()
            .<RegistryEntry<StatusEffect>>map(StatusEffectInstance::getEffectType)
            .collect(Collectors.toSet());

      for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
         cachedEffects.put(effect.getEffectType(), effect);
         if (!animatedAlphas.containsKey(effect.getEffectType())) {
            Animation2 newAnim = new Animation2();
            newAnim.set(0.0);
            animatedAlphas.put(effect.getEffectType(), newAnim);
         }
      }

      animatedAlphas.forEach((effectType, anim) -> {
         double targetValue = activeEffects.contains(effectType) ? 1.0 : 0.0;
         anim.run(targetValue, 0.6, Easings.QUART_OUT, true);
         anim.update();
      });
      Set<RegistryEntry<StatusEffect>> effectsToRender = new HashSet<>(activeEffects);
      animatedAlphas.forEach((effectType, anim) -> {
         if (anim.get() > 0.01F) {
            effectsToRender.add(effectType);
         }
      });
      List<RegistryEntry<StatusEffect>> sortedEffects = new ArrayList<>(effectsToRender);
      sortedEffects.sort((a, b) -> {
         boolean aActive = activeEffects.contains(a);
         boolean bActive = activeEffects.contains(b);
         if (aActive != bActive) {
            return aActive ? -1 : 1;
         }
         return 0;
      });

      float preferredX = 20.0F;
      float preferredY = 474.0F;

      float measureOffset = 0.0F;
      float measureMaxWidth = 0.0F;
      for (RegistryEntry<StatusEffect> effectType : sortedEffects) {
         StatusEffectInstance effectMeasure = cachedEffects.get(effectType);
         Animation2 alphaAnimMeasure = animatedAlphas.get(effectType);
         if (effectMeasure != null && alphaAnimMeasure != null && alphaAnimMeasure.get() > 0.01F) {
            String name = formatName(effectMeasure);
            String duration = formatDuration(activeEffects.contains(effectType) ? effectMeasure.getDuration() : 0);
            measureMaxWidth = Math.max(measureMaxWidth, rowWidth(r2, name, duration));
            measureOffset += (ROW_H + ROW_GAP) * alphaAnimMeasure.get();
         }
      }
      float boundsWidth = Math.max(measureMaxWidth, 120.0F);
      float boundsHeight = Math.max(measureOffset, 30.0F);

      DraggableManager.DragSession dragSession = DraggableManager.getInstance()
            .beginDrag("potions", preferredX, preferredY, boundsWidth, boundsHeight);
      float x = dragSession.positionX();
      float y = dragSession.positionY();
      float offset = 0.0F;
      float maxRenderedWidth = 0.0F;

      for (RegistryEntry<StatusEffect> effectType : sortedEffects) {
         StatusEffectInstance effect = cachedEffects.get(effectType);
         Animation2 alphaAnim = animatedAlphas.get(effectType);
         if (effect == null || alphaAnim == null) {
            continue;
         }
         float currentAlpha = alphaAnim.get();
         float x3 = -80.0F + 80.0F * currentAlpha;
         float targetY = y + offset;
         float currentAnimatedY = animatedY.getOrDefault(effectType, targetY);
         currentAnimatedY = AnimationMath.animation(currentAnimatedY, targetY, 0.1F);
         animatedY.put(effectType, currentAnimatedY);
         offset += (ROW_H + ROW_GAP) * currentAlpha;
         if (currentAlpha <= 0.01F) {
            continue;
         }

         boolean active = activeEffects.contains(effectType);
         int currentDuration = active ? effect.getDuration() : 0;
         if (!maxDurations.containsKey(effectType) || currentDuration > maxDurations.get(effectType)) {
            maxDurations.put(effectType, currentDuration);
         }

         String name = formatName(effect);
         String durationText = formatDuration(currentDuration);
         float rowW = rowWidth(r2, name, durationText);
         maxRenderedWidth = Math.max(maxRenderedWidth, rowW);
         float rowCenterY = currentAnimatedY + ROW_H * 0.5F;

         r2.pushAlpha(currentAlpha);
         GlassStyle.card(r2, x + x3, currentAnimatedY, rowW, ROW_H, 1.0F);

         float textX = x + x3 + PAD_X;
         int nameCodepoint = name.codePointAt(0);
         float nameBaseline = rowCenterY
               + FontRegistry.centeredBaselineOffset(FontRegistry.INTER_MEDIUM, nameCodepoint, NAME_SIZE);
         boolean isBeneficial = effect.getEffectType().value().isBeneficial();
         int nameColor = isBeneficial ? Renderer2D.ColorUtil.getTextColor(1, 1) : HARMFUL_TEXT;
         r2.text(FontRegistry.INTER_MEDIUM, textX, nameBaseline, NAME_SIZE, name, nameColor);

         float durW = r2.measureText(FontRegistry.INTER_MEDIUM, durationText, DUR_SIZE).width;
         int durCodepoint = durationText.codePointAt(0);
         float durBaseline = rowCenterY
               + FontRegistry.centeredBaselineOffset(FontRegistry.INTER_MEDIUM, durCodepoint, DUR_SIZE);
         int durColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), 150);
         r2.text(FontRegistry.INTER_MEDIUM, x + x3 + rowW - TEXT_PAD - durW, durBaseline, DUR_SIZE, durationText,
               durColor);

         int maxDuration = maxDurations.get(effectType);
         float progress = maxDuration > 0 ? Math.max(0.0F, Math.min(1.0F, (float) currentDuration / maxDuration)) : 0.0F;
         float targetWidth = (rowW - 2.0F * PAD_X) * progress;
         float currentAnimatedWidth = animatedWidths.getOrDefault(effectType, targetWidth);
         currentAnimatedWidth = AnimationMath.animation(currentAnimatedWidth, targetWidth, 0.1F);
         animatedWidths.put(effectType, currentAnimatedWidth);
         float barY = currentAnimatedY + ROW_H - BAR_BOTTOM_OFFSET;
         int barTrack = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 46);
         r2.rect(x + x3 + PAD_X, barY, rowW - 2.0F * PAD_X, BAR_H, BAR_H * 0.5F, barTrack);
         if (currentAnimatedWidth > 1.0F) {
            int barColor = effect.getEffectType().value().getColor() | 0xFF000000;
            r2.rect(x + x3 + PAD_X, barY, currentAnimatedWidth, BAR_H, BAR_H * 0.5F, barColor);
         }
         r2.popAlpha();
      }

      maxDurations.keySet()
            .removeIf(key -> mc.player.getStatusEffects().stream().noneMatch(e -> e.getEffectType().equals(key)));
      animatedWidths.keySet()
            .removeIf(key -> mc.player.getStatusEffects().stream().noneMatch(e -> e.getEffectType().equals(key)));
      animatedY.keySet()
            .removeIf(key -> mc.player.getStatusEffects().stream().noneMatch(e -> e.getEffectType().equals(key)));
      cachedEffects.keySet().removeIf(key -> !activeEffects.contains(key)
            && (animatedAlphas.get(key) == null || animatedAlphas.get(key).get() <= 0.01F));
      animatedAlphas.keySet().removeIf(key -> {
         Animation2 anim = animatedAlphas.get(key);
         return anim == null || (anim.get() <= 0.01F && !activeEffects.contains(key));
      });

      boundsWidth = Math.max(maxRenderedWidth, boundsWidth);
      boundsHeight = Math.max(offset, boundsHeight);
      HudEditor.registerRect(x, y, boundsWidth, boundsHeight);
      DraggableManager.getInstance().endDrag(dragSession);
   }

   private static float rowWidth(Renderer2D r2, String name, String duration) {
      float nameW = r2.measureText(FontRegistry.INTER_MEDIUM, name, NAME_SIZE).width;
      float durW = r2.measureText(FontRegistry.INTER_MEDIUM, duration, DUR_SIZE).width;
      return PAD_X + nameW + TEXT_PAD + durW + TEXT_PAD;
   }

   private static String formatName(StatusEffectInstance effect) {
      String key = effect.getTranslationKey();
      String name = key.replace("effect.minecraft.", "");
      if (name.isEmpty()) {
         name = "Effect";
      }
      name = name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
      return name + formatLevel(effect.getAmplifier());
   }

   private static String formatLevel(int amplifier) {
      int level = amplifier + 1;
      if (level <= 1) {
         return "";
      }
      String[] roman = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
      if (level <= roman.length) {
         return " " + roman[level - 1];
      }
      return " " + level;
   }

   private static String formatDuration(int ticks) {
      int totalSeconds = Math.max(0, (int) Math.ceil(ticks / 20.0));
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;
      return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
   }

}