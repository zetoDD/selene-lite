#version 330 core
in vec2 vLocalPx;
in vec2 vSize;
flat in vec4 vColorTL;
flat in vec4 vColorTR;
flat in vec4 vColorBR;
flat in vec4 vColorBL;
flat in vec4 vRadii;
flat in uint vFlags;
flat in ivec4 vClip;
flat in vec4 vClipRadii;
in vec2 vUV;
flat in vec2 vUvScale;
flat in vec2 vUvOffset;
flat in int vTexSlot;
in vec2 vPosPx;

uniform sampler2D uTextures[16];

out vec4 FragColor;

const float PI = 3.14159265;
const float TAU = 6.2831853;

vec4 sampleTexture(int slot, vec2 uv) {
    switch (slot) {
        case 0:  return texture(uTextures[0], uv);
        case 1:  return texture(uTextures[1], uv);
        case 2:  return texture(uTextures[2], uv);
        case 3:  return texture(uTextures[3], uv);
        case 4:  return texture(uTextures[4], uv);
        case 5:  return texture(uTextures[5], uv);
        case 6:  return texture(uTextures[6], uv);
        case 7:  return texture(uTextures[7], uv);
        case 8:  return texture(uTextures[8], uv);
        case 9:  return texture(uTextures[9], uv);
        case 10: return texture(uTextures[10], uv);
        case 11: return texture(uTextures[11], uv);
        case 12: return texture(uTextures[12], uv);
        case 13: return texture(uTextures[13], uv);
        case 14: return texture(uTextures[14], uv);
        case 15: return texture(uTextures[15], uv);
        default: return vec4(0.0);
    }
}

ivec2 textureDimensions(int slot) {
    switch (slot) {
        case 0:  return textureSize(uTextures[0], 0);
        case 1:  return textureSize(uTextures[1], 0);
        case 2:  return textureSize(uTextures[2], 0);
        case 3:  return textureSize(uTextures[3], 0);
        case 4:  return textureSize(uTextures[4], 0);
        case 5:  return textureSize(uTextures[5], 0);
        case 6:  return textureSize(uTextures[6], 0);
        case 7:  return textureSize(uTextures[7], 0);
        case 8:  return textureSize(uTextures[8], 0);
        case 9:  return textureSize(uTextures[9], 0);
        case 10: return textureSize(uTextures[10], 0);
        case 11: return textureSize(uTextures[11], 0);
        case 12: return textureSize(uTextures[12], 0);
        case 13: return textureSize(uTextures[13], 0);
        case 14: return textureSize(uTextures[14], 0);
        case 15: return textureSize(uTextures[15], 0);
        default: return ivec2(1, 1);
    }
}

float cornerRadius(vec2 p, vec4 radii){
    float right = step(0.0, p.x);
    float bottom = step(0.0, p.y);
    float top = mix(radii.x, radii.y, right);
    float bottomRadius = mix(radii.w, radii.z, right);
    return mix(top, bottomRadius, bottom);
}

float sdRoundBox(vec2 p, vec2 halfSize, vec4 radii){
    vec4 safeRadii = max(radii, vec4(0.0));
    float minHalf = min(halfSize.x, halfSize.y);
    
    float minR = min(min(safeRadii.x, safeRadii.y), min(safeRadii.z, safeRadii.w));
    if (minR >= minHalf - 0.5) {
        vec2 offset = max(halfSize - vec2(minHalf), vec2(0.0));
        float d = length(max(abs(p) - offset, vec2(0.0))) - minHalf;
        return d;
    }
    
    vec4 clampedRadii = min(safeRadii, vec4(minHalf));
    float radius = cornerRadius(p, clampedRadii);
    vec2 inset = max(halfSize - vec2(radius), vec2(0.0));
    vec2 q = abs(p) - inset;
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

float lengthNO(vec2 v, float n) {
    v = abs(v);
    return pow(pow(v.x, n) + pow(v.y, n), 1.0 / n);
}

float roundedBoxSDFO(vec2 p, vec2 halfSize, vec4 r, float n) {
    n = max(n, 1.0);
    float rad = (p.x >= 0.0)
        ? ((p.y >= 0.0) ? r.x : r.y)
        : ((p.y >= 0.0) ? r.w : r.z);

    vec2 q = abs(p) - halfSize + rad;
    vec2 qpos = max(q, 0.0);

    float d = lengthNO(qpos, n) + min(max(q.x, q.y), 0.0) - rad;
    return d;
}


float sdCircle(vec2 p, float r){
    return length(p) - r;
}

float median(float r, float g, float b){
    return max(min(r, g), min(max(r, g), b));
}

void main(){
    uint mode = vFlags & 3u;
    float thickness = float((vFlags >> 2) & 0xFFu);
    float startRad = float((vFlags >> 10) & 0xFFu) / 255.0 * TAU;
    float arcPct = float((vFlags >> 18) & 0xFFu) / 255.0;
    float safeWx = (abs(vSize.x) > 1e-6) ? vSize.x : (vSize.x >= 0.0 ? 1e-6 : -1e-6);
    float safeHy = (abs(vSize.y) > 1e-6) ? vSize.y : (vSize.y >= 0.0 ? 1e-6 : -1e-6);
    vec2 gradUV = clamp(vLocalPx / vec2(safeWx, safeHy), 0.0, 1.0);
    vec4 colTop = mix(vColorTL, vColorTR, gradUV.x);
    vec4 colBottom = mix(vColorBL, vColorBR, gradUV.x);
    vec4 col = mix(colTop, colBottom, gradUV.y);
    vec2 halfSize = 0.5 * vSize;
    vec2 p = vLocalPx - halfSize;
    float d = 0.0;

    if (vClip.z <= 0 || vClip.w <= 0) {
        discard;
    }

    if (vPosPx.x < float(vClip.x) || vPosPx.y < float(vClip.y)
    || vPosPx.x >= float(vClip.x + vClip.z) || vPosPx.y >= float(vClip.y + vClip.w)) {
        discard;
    }

    float clipMask = 1.0;
    vec4 clipRadii = max(vClipRadii, vec4(0.0));
    if (clipRadii.x + clipRadii.y + clipRadii.z + clipRadii.w > 1e-6) {
        vec2 clipSize = vec2(float(vClip.z), float(vClip.w));
        if (clipSize.x <= 0.0 || clipSize.y <= 0.0) {
            discard;
        }
        vec2 clipHalf = clipSize * 0.5;
        vec2 clipCenter = vec2(float(vClip.x), float(vClip.y)) + clipHalf;
        vec2 clipLocal = vPosPx - clipCenter;
        float clipDistance = sdRoundBox(clipLocal, clipHalf, clipRadii);
        float clipFeather = max(fwidth(clipDistance), 1e-3);
        clipMask = 1.0 - smoothstep(0.0, clipFeather, clipDistance);
        if (clipMask <= 0.0) {
            discard;
        }
    }

    vec4 cornerRadii = max(vRadii, vec4(0.0));
    vec3 baseRgb = col.rgb;
    float baseAlpha = col.a;

    bool shadowMode = ((vFlags >> 26) & 0x1u) == 1u;
    if (shadowMode) {
        vec2 innerSizeRaw = vUvScale;
        vec2 resolvedInner = vec2(
            innerSizeRaw.x > 0.0 ? innerSizeRaw.x : vSize.x,
            innerSizeRaw.y > 0.0 ? innerSizeRaw.y : vSize.y
        );
        vec2 innerHalf = max(resolvedInner * 0.5, vec2(0.0));
        vec2 pad = 0.5 * (vSize - resolvedInner);
        vec2 center = pad + innerHalf;
        vec2 local = vLocalPx - center;
        vec4 innerRadii = cornerRadii;

        float distInner = sdRoundBox(local, innerHalf, innerRadii);
        float aaInner = max(fwidth(distInner), 1e-4);
        float innerMask = clamp((distInner + aaInner) / aaInner, 0.0, 1.0);
        if (innerMask <= 0.0) {
            discard;
        }

        float blur = max(vUvOffset.x, 1e-3);
        float spread = max(vUvOffset.y, 0.0);
        float distFromSpread = max(distInner - spread, 0.0);
        float norm = distFromSpread / blur;
        float gaussian = exp(-0.5 * norm * norm);

        float limit = blur * 3.0;
        float outerMask = 1.0;
        if (limit > 0.0) {
            float aaOuter = max(fwidth(distFromSpread), 1e-4);
            outerMask = 1.0 - smoothstep(limit - aaOuter, limit + aaOuter, distFromSpread);
        }

        float alpha = baseAlpha * gaussian * innerMask * outerMask * clipMask;
        if (alpha <= 0.001) {
            discard;
        }
        vec3 rgb = baseRgb * alpha;
        FragColor = vec4(rgb, alpha);
        return;
    }

    if (mode == 3u) {
        if (vTexSlot < 0 || vTexSlot >= 16) discard;

        bool isMsdf = ((vFlags >> 4) & 0x1u) == 1u;
        bool useScreenSpaceUv = ((vFlags >> 5) & 0x1u) == 1u;
        bool preservePremultiplied = ((vFlags >> 6) & 0x1u) == 1u;
        bool isMsdfOutline = ((vFlags >> 7) & 0x1u) == 1u;
        vec2 sampleUv;
        if (useScreenSpaceUv) {
            float u = clamp(vPosPx.x * vUvScale.x + vUvOffset.x, 0.0, 1.0);
            float v = clamp(vPosPx.y * vUvScale.y + vUvOffset.y, 0.0, 1.0);
            sampleUv = vec2(u, v);
        } else {
            sampleUv = vUV;
        }
        vec4 tex = sampleTexture(vTexSlot, sampleUv);

        if (isMsdf) {
            float pxRange = max(cornerRadii.x, 1e-6);
            vec2 atlasSize = vec2(textureDimensions(vTexSlot));
            vec2 unitRange = vec2(pxRange) / max(atlasSize, vec2(1.0));
            vec2 screenTexSize = vec2(1.0) / fwidth(vUV);
            float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
            float dist = median(tex.r, tex.g, tex.b);
            float pxDist = screenPxRange * (dist - 0.5);
            float glyphOpacity = clamp(pxDist * 1.28 + 0.5, 0.0, 1.0);
            float opacity;
            if (isMsdfOutline) {
                float outlineWidth = max(cornerRadii.y, 0.0);
                float hollowOpacity = clamp((pxDist - outlineWidth) * 1.28 + 0.5, 0.0, 1.0);
                opacity = max(glyphOpacity - hollowOpacity, 0.0);
            } else {
                opacity = glyphOpacity;
            }
            float alpha = col.a * opacity;
            vec3 rgb = col.rgb * alpha;
            if (alpha <= 0.001) discard;
            FragColor = vec4(rgb, alpha);
            return;
        }

        bool isRGBA = ((vFlags >> 2) & 0x1u) == 1u;
        bool forceOpaque = ((vFlags >> 3) & 0x1u) == 1u;

        vec2 dx = dFdx(vLocalPx);
        vec2 dy = dFdy(vLocalPx);
        float px = 0.5 * (length(dx) + length(dy)) + 1e-6;
        vec2 offs[4] = vec2[4](
            vec2(-0.33, -0.33), vec2(0.33, -0.33), vec2(0.33, 0.33), vec2(-0.33, 0.33)
        );
        float acc = 0.0;
        for (int i = 0; i < 4; i++) {
            vec2 ps = (vLocalPx + dx * offs[i].x + dy * offs[i].y) - (0.5 * vSize);
            float dS = sdRoundBox(ps, 0.5 * vSize, cornerRadii);
            float a = 1.0 - smoothstep(0.0, px, dS);
            acc += a;
        }
        float mask = acc * 0.25;

        vec4 sampled;
        if (isRGBA) {
            sampled = vec4(tex.rgb, forceOpaque ? 1.0 : tex.a);
        } else {
            float cov = tex.r;
            cov = pow(cov, 1.0/1.6);
            sampled = vec4(cov, cov, cov, cov);
        }
        vec4 colTex = sampled * col;
        if (!preservePremultiplied) {
            colTex.rgb *= colTex.a;
        }
        colTex *= mask * clipMask;

        if (colTex.a <= 0.001) discard;
        FragColor = colTex;
        return;
    } else if (mode == 2u) {

        vec2 dx = dFdx(vLocalPx);
        vec2 dy = dFdy(vLocalPx);
        float px = 0.5 * (length(dx) + length(dy)) + 1e-6;

        vec2 offs[8] = vec2[8](
            vec2(-0.25, -0.25), vec2(0.25, -0.25), vec2(0.25, 0.25), vec2(-0.25, 0.25),
            vec2(-0.5, 0.0), vec2(0.5, 0.0), vec2(0.0, -0.5), vec2(0.0, 0.5)
        );
        float acc = 0.0;
        float radius = halfSize.x;
        float sectorWidth = arcPct * TAU;

        float betterPx = max(px * 0.7, 0.5);

        for (int i = 0; i < 8; i++) {
            vec2 ps = p + dx * offs[i].x + dy * offs[i].y;
            float dC = length(ps) - radius;
            float aR = (thickness > 0.0)
                ? (1.0 - smoothstep(thickness - betterPx, thickness + betterPx, abs(dC)))
                : (1.0 - smoothstep(-betterPx, betterPx, dC));

            float aA = 1.0;
            if (sectorWidth < TAU - 1e-6) {
                float ang = atan(ps.y, ps.x);
                if (ang < 0.0) ang += TAU;
                float rel = mod(ang - startRad + TAU, TAU);
                float center = sectorWidth * 0.5;
                float delta = max(abs(rel - center) - center, 0.0);
                float sAng = radius * delta;
                aA = 1.0 - smoothstep(0.0, betterPx, sAng);
            }
            acc += aR * aA;
        }
        col.a *= acc * 0.125;
        col.a *= clipMask;
    } else if (mode == 1u) {
        float Smoothness = 1.0;
        float CornerSmoothness = 2.0;

        vec2 halfSizePix = halfSize - 0.5;

        float dOuter = roundedBoxSDFO(p, halfSizePix, cornerRadii, CornerSmoothness);
        float aa = max(fwidth(dOuter), 1e-4) * Smoothness;

        vec2 halfInner = max(halfSizePix - thickness, 0.0);
        vec4 innerRadius = max(cornerRadii - thickness, 0.0);
        float dInner = roundedBoxSDFO(p, halfInner, innerRadius, CornerSmoothness);

        float alphaOuter = 1.0 - smoothstep(-aa, aa, dOuter);
        float alphaInner = 1.0 - smoothstep(-aa, aa, dInner);

        col.a *= clamp(alphaOuter - alphaInner, 0.0, 1.0);
        col.a *= clipMask;
    } else {
        vec2 dx = dFdx(vLocalPx);
        vec2 dy = dFdy(vLocalPx);
        float px = 0.5 * (length(dx) + length(dy)) + 1e-6;
        
        float minHalf = min(halfSize.x, halfSize.y);
        float minR = min(min(cornerRadii.x, cornerRadii.y), min(cornerRadii.z, cornerRadii.w));
        
        if (minR >= minHalf - 0.5) {
            vec2 offset = max(halfSize - vec2(minHalf), vec2(0.0));
            float d = length(max(abs(p) - offset, vec2(0.0))) - minHalf;
            float a = 1.0 - smoothstep(-px, px, d);
            col.a *= a;
            col.a *= clipMask;
        } else {
            vec2 offs[4] = vec2[4](
                vec2(-0.33, -0.33), vec2(0.33, -0.33), vec2(0.33, 0.33), vec2(-0.33, 0.33)
            );
            float acc = 0.0;
            for (int i = 0; i < 4; i++) {
                vec2 ps = p + dx * offs[i].x + dy * offs[i].y;
                float dS = sdRoundBox(ps, halfSize, cornerRadii);
                float a = 1.0 - smoothstep(0.0, px, dS);
                acc += a;
            }
            col.a *= acc * 0.25;
            col.a *= clipMask;
        }
    }

    col.rgb *= col.a;
    if (col.a <= 0.001) discard;
    FragColor = col;
}

