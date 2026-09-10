package sl.selene.mixin;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sl.selene.commands.CommandManager;
import sl.selene.commands.suggestions.CommandSuggestionManager;
import sl.selene.commands.suggestions.CommandSuggestionOverlay;
import sl.selene.commands.suggestions.CommandSuggestions;
import sl.selene.module.impl.visuals.HUD.HudEditor;

@Environment(EnvType.CLIENT)
@Mixin({ChatScreen.class})
public class ChatScreenMixin {
   private static final int COMMAND_SUGGESTION_MAX_VISIBLE = 6;
   @Shadow
   private TextFieldWidget chatField;
   @Unique
   private CommandSuggestions.SuggestionSet commandSuggestions;
   @Unique
   private int commandSuggestionSelection;
   @Unique
   private int commandSuggestionScroll;
   @Unique
   private CommandSuggestionOverlay.Layout commandSuggestionLayout;
   @Unique
   private long lastScrollTime;

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void onChatOpen(CallbackInfo ci) {
   }

   @Inject(
      method = {"removed"},
      at = {@At("HEAD")}
   )
   private void onChatClose(CallbackInfo ci) {
      this.clearSuggestionState();
      HudEditor.closeAllPopups();
   }

   @Inject(
      method = {"sendMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleConfigCommands(String message, boolean addToHistory, CallbackInfo ci) {
      if (CommandManager.getInstance().handleMessage(message)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"onChatFieldUpdate"},
      at = {@At("HEAD")}
   )
   private void onChatUpdateHead(String chatText, CallbackInfo ci) {
      if (this.chatField == null || !CommandSuggestionManager.supportsInput(chatText)) {
         this.clearSuggestionState();
      }
   }

   @Inject(
      method = {"onChatFieldUpdate"},
      at = {@At("TAIL")}
   )
   private void onChatUpdateTail(String chatText, CallbackInfo ci) {
      if (this.chatField != null && CommandSuggestionManager.supportsInput(chatText)) {
         CommandSuggestions.SuggestionSet suggestions = CommandSuggestionManager.collect(chatText);
         if (suggestions != null && !suggestions.isEmpty()) {
            this.commandSuggestions = suggestions;
            this.commandSuggestionSelection = 0;
            this.commandSuggestionScroll = 0;
            this.updateSuggestionHint();
         } else {
            this.clearSuggestionState();
         }
      }
   }

   @Inject(
      method = {"keyPressed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleSuggestionKeys(net.minecraft.client.input.KeyInput input, CallbackInfoReturnable<Boolean> cir) {
      int keyCode = input.key();
      int scanCode = input.scancode();
      int modifiers = input.modifiers();
      if (this.hasCommandSuggestions() && this.chatField != null) {
         if (keyCode != 258) {
            if (keyCode == 265) {
               if (this.moveSelection(-1)) {
                  cir.setReturnValue(true);
               }
            } else {
               if (keyCode == 264 && this.moveSelection(1)) {
                  cir.setReturnValue(true);
               }
            }
         } else {
            if (input.hasShift()) {
               if (this.moveSelection(-1)) {
                  cir.setReturnValue(true);
               } else {
                  cir.setReturnValue(true);
               }
            } else if (this.applySelectedSuggestion()) {
               cir.setReturnValue(true);
            } else if (this.getSuggestionCount() > 1 && this.moveSelection(1)) {
               cir.setReturnValue(true);
            } else {
               cir.setReturnValue(true);
            }
         }
      }
   }

   @Inject(
      method = {"mouseScrolled"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleSuggestionScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
      if (this.hasCommandSuggestions() && this.commandSuggestionLayout != null) {
         if (this.commandSuggestionLayout.contains(mouseX, mouseY)) {
            if (verticalAmount > 0.0) {
               if (this.scrollSuggestions(-1)) {
                  this.lastScrollTime = System.currentTimeMillis();
                  cir.setReturnValue(true);
               }
            } else if (verticalAmount < 0.0 && this.scrollSuggestions(1)) {
               this.lastScrollTime = System.currentTimeMillis();
               cir.setReturnValue(true);
            }
         }
      }
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleSuggestionClick(net.minecraft.client.gui.Click click, boolean consumed, CallbackInfoReturnable<Boolean> cir) {
      double mouseX = click.x();
      double mouseY = click.y();
      int button = click.button();
      if (button == 0 && this.hasCommandSuggestions() && this.commandSuggestionLayout != null) {
         int entryIndex = this.commandSuggestionLayout.entryIndexAt(mouseX, mouseY);
         if (entryIndex >= 0 && entryIndex < this.getSuggestionCount()) {
            this.setSelection(entryIndex, false);
            this.applySelectedSuggestion();
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void renderSuggestions(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.hasCommandSuggestions() && this.chatField != null) {
         List<CommandSuggestions.SuggestionEntry> entries = this.commandSuggestions.entries();
         if (entries.isEmpty()) {
            this.commandSuggestionLayout = null;
         } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
               this.commandSuggestionLayout = null;
            } else {
               TextRenderer renderer = client.textRenderer;
               int screenWidth = client.getWindow().getScaledWidth();
               CommandSuggestionOverlay.Layout layout = CommandSuggestionOverlay.render(
                  context, renderer, this.chatField, screenWidth, entries, this.commandSuggestionSelection, this.commandSuggestionScroll, 6, mouseX, mouseY
               );
               this.commandSuggestionLayout = layout;
               if (layout != null) {
                  this.commandSuggestionScroll = layout.scrollOffset();
                  int hoveredIndex = layout.entryIndexAt(mouseX, mouseY);
                  if (hoveredIndex >= 0) {
                     this.setSelection(hoveredIndex, false);
                  }
               }
            }
         }
      } else {
         this.commandSuggestionLayout = null;
      }
   }

   @Unique
   private void clearSuggestionState() {
      this.commandSuggestions = null;
      this.commandSuggestionSelection = 0;
      this.commandSuggestionScroll = 0;
      this.commandSuggestionLayout = null;
      if (this.chatField != null) {
         this.chatField.setSuggestion(null);
      }
   }

   @Unique
   private boolean hasCommandSuggestions() {
      return this.commandSuggestions != null && !this.commandSuggestions.entries().isEmpty();
   }

   @Unique
   private void updateSuggestionHint() {
      if (this.chatField != null) {
         String suffix = this.getSelectedSuffix();
         this.chatField.setSuggestion(suffix);
      }
   }

   @Unique
   private String getSelectedSuffix() {
      if (!this.hasCommandSuggestions()) {
         return null;
      } else {
         List<CommandSuggestions.SuggestionEntry> entries = this.commandSuggestions.entries();
         if (entries.isEmpty()) {
            return null;
         } else {
            int index = MathHelper.clamp(this.commandSuggestionSelection, 0, entries.size() - 1);
            CommandSuggestions.SuggestionEntry entry = entries.get(index);
            return entry.completionSuffix();
         }
      }
   }

   @Unique
   private boolean moveSelection(int delta) {
      if (!this.hasCommandSuggestions()) {
         return false;
      } else {
         List<CommandSuggestions.SuggestionEntry> entries = this.commandSuggestions.entries();
         if (entries.isEmpty()) {
            return false;
         } else {
            int size = entries.size();
            int index = Math.floorMod(this.commandSuggestionSelection + delta, size);
            this.setSelection(index, true);
            return true;
         }
      }
   }

   @Unique
   private void setSelection(int index, boolean adjustScroll) {
      if (this.hasCommandSuggestions()) {
         List<CommandSuggestions.SuggestionEntry> entries = this.commandSuggestions.entries();
         if (!entries.isEmpty()) {
            int clampedIndex = MathHelper.clamp(index, 0, entries.size() - 1);
            this.commandSuggestionSelection = clampedIndex;
            if (adjustScroll) {
               this.ensureSelectionVisible(entries.size());
            }

            this.updateSuggestionHint();
         }
      }
   }

   @Unique
   private boolean scrollSuggestions(int delta) {
      if (!this.hasCommandSuggestions()) {
         return false;
      } else {
         int totalEntries = this.getSuggestionCount();
         if (totalEntries <= 0) {
            return false;
         } else {
            int maxVisible = Math.max(1, 6);
            int maxOffset = Math.max(0, totalEntries - maxVisible);
            if (maxOffset == 0) {
               return false;
            } else {
               int newOffset = MathHelper.clamp(this.commandSuggestionScroll + delta, 0, maxOffset);
               if (newOffset == this.commandSuggestionScroll) {
                  return false;
               } else {
                  this.commandSuggestionScroll = newOffset;
                  return true;
               }
            }
         }
      }
   }

   @Unique
   private void ensureSelectionVisible(int totalEntries) {
      int maxVisible = Math.max(1, 6);
      int visible = Math.min(totalEntries, maxVisible);
      if (visible <= 0) {
         this.commandSuggestionScroll = 0;
      } else {
         if (this.commandSuggestionSelection < this.commandSuggestionScroll) {
            this.commandSuggestionScroll = this.commandSuggestionSelection;
         } else if (this.commandSuggestionSelection >= this.commandSuggestionScroll + visible) {
            this.commandSuggestionScroll = this.commandSuggestionSelection - visible + 1;
         }

         int maxOffset = Math.max(0, totalEntries - visible);
         if (this.commandSuggestionScroll > maxOffset) {
            this.commandSuggestionScroll = maxOffset;
         }

         if (this.commandSuggestionScroll < 0) {
            this.commandSuggestionScroll = 0;
         }
      }
   }

   @Unique
   private boolean applySelectedSuggestion() {
      String suffix = this.getSelectedSuffix();
      if (suffix != null && !suffix.isEmpty()) {
         this.chatField.write(suffix);
         return true;
      } else {
         return false;
      }
   }

   @Unique
   private int getSuggestionCount() {
      return this.hasCommandSuggestions() ? this.commandSuggestions.entries().size() : 0;
   }
}