#version 330 core

in vec2 vLocalPx;
in vec2 vPosPx;
flat in vec2 vSize;
flat in vec4 vRadii;
flat in vec4 vAlphaPowerMix;
flat in vec4 vFresnel;
flat in vec4 vFlags;

uniform sampler2D uBlur;
uniform vec2 uBlurScale;
uniform vec2 uBlurOffset;
uniform vec4 uScissor;
uniform float uScissorEnabled;

out vec4 FragColor;

float rdist(vec2 pos, vec2 size, vec4 radius) {
    float cornerRadius;
    if (pos.x > 0.0) {
        cornerRadius = (pos.y > 0.0) ? radius.x : radius.w;
    } else {
        cornerRadius = (pos.y > 0.0) ? radius.y : radius.z;
    }

    vec2 v = abs(pos) - size + cornerRadius;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - cornerRadius;
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float feather = max(smoothness, 0.001);
    float dist = rdist(center - coord * size, max(center - 1.0, vec2(0.0)), max(radius, vec4(0.0)));
    return 1.0 - smoothstep(1.0 - feather, 1.0, dist);
}

// Outward-pointing surface normal from the SDF gradient (central differences).
vec2 sdfNormal(vec2 p, vec2 halfSize, vec4 radius) {
    float eps = 1.0;
    vec2 g = vec2(
        rdist(p + vec2(eps, 0.0), halfSize, radius) - rdist(p - vec2(eps, 0.0), halfSize, radius),
        rdist(p + vec2(0.0, eps), halfSize, radius) - rdist(p - vec2(0.0, eps), halfSize, radius)
    );
    return (length(g) > 0.0001) ? normalize(g) : vec2(0.0, 1.0);
}

void main() {
    if (uScissorEnabled > 0.5) {
        if (vPosPx.x < uScissor.x || vPosPx.y < uScissor.y
            || vPosPx.x > uScissor.z || vPosPx.y > uScissor.w) {
            discard;
        }
    }

    vec2 coord = clamp(vLocalPx / vSize, vec2(0.0), vec2(1.0));
    vec2 size = max(vSize, vec2(1.0));
    float smoothness = 0.5;
    float globalAlpha = clamp(vAlphaPowerMix.x, 0.0, 1.0);
    float fresnelMix = clamp(vAlphaPowerMix.w, 0.0, 1.0);
    float baseAlpha = clamp(vAlphaPowerMix.z, 0.0, 1.0);
    float lensStrengthPx = max(vFlags.y, 0.0); // DISTORT = max inward displacement in px

    float alpha = ralpha(size, coord, vRadii, smoothness);

    vec2 center = size * 0.5;
    vec2 halfSize = max(center - 1.0, vec2(0.0));
    vec2 pos = center - coord * size;       // relative to shape center; dist<=0 inside
    float dist = rdist(pos, halfSize, vRadii);
    vec2 normal = sdfNormal(pos, halfSize, vRadii);

    // --- 1. Lens refraction: displace the backdrop sample inward, strongest at the edge,
    //        fading to ~nothing toward the center (real refraction, not blur).
    float lensPx = 20.0;                      // width of the refractive rim band
    float edgeDist = clamp(-dist, 0.0, lensPx);
    float lensT = 1.0 - edgeDist / lensPx;    // 1 at edge, 0 past the band
    lensT = lensT * lensT * (3.0 - 2.0 * lensT);
    float dispPx = lensT * lensStrengthPx;
    vec2 dispUv = -normal * dispPx * uBlurScale; // inward displacement in uv space

    vec2 uv = clamp(vPosPx * uBlurScale + uBlurOffset, 0.0, 1.0);
    // Chromatic dispersion: sample R/G/B at slightly different offsets for a faint fringe.
    vec3 bg;
    bg.r = texture(uBlur, uv + dispUv * 1.02).r;
    bg.g = texture(uBlur, uv + dispUv).g;
    bg.b = texture(uBlur, uv + dispUv * 0.98).b;

    // --- 2. Directional specular rim: bright thin line only where the edge faces the light
    //        (upper-left), so it reads as a single light source, not an even glow.
    vec2 lightDir = normalize(vec2(-0.45, -0.9)); // in pixel space, y down => up-left
    float facing = clamp(dot(normal, -lightDir), 0.0, 1.0);
    float edgeBand = 1.0 - smoothstep(0.0, 1.8, edgeDist);
    // seedPower flattens how fast the rim falls away around the sides.
    float rim = edgeBand * pow(facing, 3.0);
    rim *= clamp(fresnelMix * 0.9, 0.0, 0.5);

    // --- 3. Near-colorless body: barely whiten toward the center; color comes from the
    //        backdrop it refracts. vFresnel is the rim highlight colour.
    float centerT = 1.0 - lensT;
    vec3 body = mix(bg.rgb, vec3(1.0), 0.10 * centerT);
    vec3 rimCol = vec3(vFresnel.rgb) * rim * max(vFresnel.a, 0.0) * 0.55;
    vec3 color = body + rimCol;

    float finalAlpha = (baseAlpha + rim) * alpha * globalAlpha;
    if (finalAlpha < 0.001) {
        discard;
    }

    FragColor = vec4(color * finalAlpha, finalAlpha);
}