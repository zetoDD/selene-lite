#version 330 core

in vec2 vLocalPx;
in vec2 vPosPx;
flat in vec2 vSize;
flat in vec4 vRadii;
flat in vec4 vAlphaPowerMix;
flat in vec4 vFresnel;
flat in vec4 vFlags;
flat in vec4 vTint;

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

float outlineAlpha(vec2 coord, vec2 size, vec4 radius, float thickness, float smoothness) {
    float outer = ralpha(size, coord, radius, smoothness);
    vec2 innerSize = size - vec2(thickness * 2.0);
    if (innerSize.x <= 0.0 || innerSize.y <= 0.0) {
        return outer;
    }

    vec2 local = coord * size;
    vec2 innerCoord = (local - vec2(thickness)) / innerSize;
    vec4 innerRadius = max(radius - vec4(thickness), vec4(0.0));
    float inner = ralpha(innerSize, innerCoord, innerRadius, smoothness);
    return clamp(outer - inner, 0.0, 1.0);
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
    float thickness = max(vFlags.z, 0.0);
    float smoothness = 0.5;
    float globalAlpha = clamp(vAlphaPowerMix.x, 0.0, 1.0);
    float baseAlpha = clamp(vAlphaPowerMix.z, 0.0, 1.0);
    float fresnelMix = clamp(vAlphaPowerMix.w, 0.0, 1.0);

    float alpha = outlineAlpha(coord, size, vRadii, thickness, smoothness);

    vec2 center = size * 0.5;
    vec2 halfSize = max(center - 1.0, vec2(0.0));
    vec2 pos = center - coord * size;
    vec2 normal = sdfNormal(pos, halfSize, vRadii);

    // Backdrop refracted tint so the edge reads as part of the glass body.
    vec2 uv = clamp(vPosPx * uBlurScale + uBlurOffset, 0.0, 1.0);
    vec3 bg = texture(uBlur, uv).rgb;

    // Thin directional highlight: only where the border faces the single light source.
    vec2 lightDir = normalize(vec2(-0.45, -0.9)); // up-left, pixel space (y down)
    float facing = clamp(dot(normal, -lightDir), 0.0, 1.0);
    float rim = pow(facing, 2.4) * clamp(fresnelMix * 0.7, 0.0, 0.35);

    vec3 color = mix(bg, vec3(vFresnel.rgb), rim);
    color = mix(color, vec3(vTint.rgb), rim * clamp(vTint.a, 0.0, 1.0) * 0.5);

    float finalAlpha = (baseAlpha + rim) * alpha * globalAlpha;
    if (finalAlpha < 0.001) {
        discard;
    }

    FragColor = vec4(color * finalAlpha, finalAlpha);
}