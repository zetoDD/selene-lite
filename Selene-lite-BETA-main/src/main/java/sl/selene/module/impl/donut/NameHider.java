package sl.selene.module.impl.donut;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.StringSetting;

@IModule(name = "NameHider", description = "Shows a custom name everywhere your real name would appear", category = Category.Donut, bind = -1)
@Environment(EnvType.CLIENT)
public class NameHider extends Module {

   public static StringSetting customName = new StringSetting("Name", "bob");

   private static boolean active;

   public NameHider() {
      this.addSettings(new Setting[] { customName });
   }

   @Override
   public void onEnable() {
      super.onEnable();
      active = true;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      active = false;
   }

   @Override
   protected void onConfigLoadEnable() {
      active = true;
   }

   @Override
   protected void onConfigLoadDisable() {
      active = false;
   }

   public static String fakeName() {
      String value = customName.get();
      return value == null ? "" : value.trim();
   }

   public static String realName() {
      return mc.player == null ? null : mc.player.getGameProfile().name();
   }

   public static boolean isHiding() {
      if (!active || mc.player == null) {
         return false;
      }
      String fake = fakeName();
      String real = realName();
      return !fake.isEmpty() && real != null && !fake.equalsIgnoreCase(real);
   }

   public static boolean referencesRealName(String content) {
      if (!isHiding() || content == null || content.isEmpty()) {
         return false;
      }
      String real = realName();
      if (real == null || real.isEmpty()) {
         return false;
      }
      return content.toLowerCase().contains(real.toLowerCase());
   }

   public static Text hide(Text text) {
      if (text == null || !isHiding()) {
         return text;
      }
      String real = realName();
      String fake = fakeName();
      return rewrite(text, real, fake);
   }

   private static Text rewrite(Text text, String real, String fake) {
      if (text == null) {
         return null;
      }
      TextContent content = text.getContent();
      MutableText out;
      if (content instanceof PlainTextContent plain) {
         out = Text.literal(plain.string().replace(real, fake));
      } else if (content instanceof TranslatableTextContent translatable) {
         Object[] args = translatable.getArgs();
         Object[] newArgs = null;
         for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Object replacement = arg instanceof Text nested ? rewrite(nested, real, fake)
                  : arg instanceof String str ? str.replace(real, fake) : arg;
            if (replacement != arg) {
               if (newArgs == null) {
                  newArgs = args.clone();
               }
               newArgs[i] = replacement;
            }
         }
         out = Text.translatableWithFallback(translatable.getKey(), translatable.getFallback(),
               newArgs == null ? args : newArgs);
      } else {
         return text;
      }
      out.setStyle(text.getStyle());
      for (Text child : text.getSiblings()) {
         out.append(rewrite(child, real, fake));
      }
      return out;
   }
}