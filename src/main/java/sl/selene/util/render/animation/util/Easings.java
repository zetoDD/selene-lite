package sl.selene.util.render.animation.util;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Easings {
   public static final double c1 = 1.70158;
   public static final double c2 = 2.5949095;
   public static final double c3 = 2.70158;
   public static final double c4 = Math.PI * 2.0 / 3.0;
   public static final double c5 = Math.PI * 4.0 / 9.0;
   public static final Easing LINEAR = value -> value;
   public static final Easing QUAD_IN = powIn(2);
   public static final Easing QUAD_OUT = powOut(2);
   public static final Easing QUAD_IN_OUT = powIN_OUT(2.0);
   public static final Easing CUBIC_IN = powIn(3);
   public static final Easing CUBIC_OUT = powOut(3);
   public static final Easing CUBIC_IN_OUT = powIN_OUT(3.0);
   public static final Easing QUART_IN = powIn(4);
   public static final Easing QUART_OUT = powOut(4);
   public static final Easing QUART_IN_OUT = powIN_OUT(4.0);
   public static final Easing QUINT_IN = powIn(5);
   public static final Easing QUINT_OUT = powOut(5);
   public static final Easing QUINT_IN_OUT = powIN_OUT(5.0);
   public static final Easing SINE_IN = value -> 1.0 - Math.cos(value * Math.PI / 2.0);
   public static final Easing SINE_OUT = value -> Math.sin(value * Math.PI / 2.0);
   public static final Easing SINE_IN_OUT = value -> -(Math.cos(Math.PI * value) - 1.0) / 2.0;
   public static final Easing CIRC_IN = value -> 1.0 - Math.sqrt(1.0 - Math.pow(value, 2.0));
   public static final Easing CIRC_OUT = value -> Math.sqrt(1.0 - Math.pow(value - 1.0, 2.0));
   public static final Easing CIRC_IN_OUT = value -> value < 0.5
      ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * value, 2.0))) / 2.0
      : (Math.sqrt(1.0 - Math.pow(-2.0 * value + 2.0, 2.0)) + 1.0) / 2.0;
   public static final Easing ELASTIC_IN = value -> value != 0.0 && value != 1.0
      ? Math.pow(-2.0, 10.0 * value - 10.0) * Math.sin((value * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0))
      : value;
   public static final Easing ELASTIC_OUT = value -> value != 0.0 && value != 1.0
      ? Math.pow(2.0, -10.0 * value) * Math.sin((value * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0
      : value;
   public static final Easing ELASTIC_IN_OUT = value -> {
      if (value != 0.0 && value != 1.0) {
         return value < 0.5
            ? -(Math.pow(2.0, 20.0 * value - 10.0) * Math.sin((20.0 * value - 11.125) * (Math.PI * 4.0 / 9.0))) / 2.0
            : Math.pow(2.0, -20.0 * value + 10.0) * Math.sin((20.0 * value - 11.125) * (Math.PI * 4.0 / 9.0)) / 2.0 + 1.0;
      } else {
         return value;
      }
   };
   public static final Easing EXPO_IN = value -> value != 0.0 ? Math.pow(2.0, 10.0 * value - 10.0) : value;
   public static final Easing EXPO_OUT = value -> value != 1.0 ? 1.0 - Math.pow(2.0, -10.0 * value) : value;
   public static final Easing EXPO_IN_OUT = value -> {
      if (value != 0.0 && value != 1.0) {
         return value < 0.5 ? Math.pow(2.0, 20.0 * value - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * value + 10.0)) / 2.0;
      } else {
         return value;
      }
   };
   public static final Easing BACK_IN = value -> 2.70158 * Math.pow(value, 3.0) - 1.70158 * Math.pow(value, 2.0);
   public static final Easing BACK_OUT = value -> 1.0 + 2.70158 * Math.pow(value - 1.0, 3.0) + 1.70158 * Math.pow(value - 1.0, 2.0);
   public static final Easing BACK_IN_OUT = value -> value < 0.5
      ? Math.pow(2.0 * value, 2.0) * (7.189819 * value - 2.5949095) / 2.0
      : (Math.pow(2.0 * value - 2.0, 2.0) * (3.5949095 * (value * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
   public static final Easing BOUNCE_OUT = x -> {
      double n1 = 7.5625;
      double d1 = 2.75;
      if (x < 1.0 / d1) {
         return n1 * Math.pow(x, 2.0);
      } else if (x < 2.0 / d1) {
         return n1 * Math.pow(x - 1.5 / d1, 2.0) + 0.75;
      } else {
         return x < 2.5 / d1 ? n1 * Math.pow(x - 2.25 / d1, 2.0) + 0.9375 : n1 * Math.pow(x - 2.625 / d1, 2.0) + 0.984375;
      }
   };
   public static final Easing BOUNCE_IN = value -> 1.0 - BOUNCE_OUT.ease(1.0 - value);
   public static final Easing BOUNCE_IN_OUT = value -> value < 0.5
      ? (1.0 - BOUNCE_OUT.ease(1.0 - 2.0 * value)) / 2.0
      : (1.0 + BOUNCE_OUT.ease(2.0 * value - 1.0)) / 2.0;

   public static Easing powIn(double n) {
      return value -> Math.pow(value, n);
   }

   public static Easing powIn(int n) {
      return powIn((double)n);
   }

   public static Easing powOut(double n) {
      return value -> 1.0 - Math.pow(1.0 - value, n);
   }

   public static Easing powOut(int n) {
      return powOut((double)n);
   }

   public static Easing powIN_OUT(double n) {
      return value -> value < 0.5 ? Math.pow(2.0, n - 1.0) * Math.pow(value, n) : 1.0 - Math.pow(-2.0 * value + 2.0, n) / 2.0;
   }

   @Generated
   private Easings() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}