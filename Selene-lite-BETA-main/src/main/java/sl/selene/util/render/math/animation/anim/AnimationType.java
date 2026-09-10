package sl.selene.util.render.math.animation.anim;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum AnimationType {
   BEZIER,
   EASING;
}