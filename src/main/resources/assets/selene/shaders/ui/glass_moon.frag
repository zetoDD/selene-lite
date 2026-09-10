#version 330 core

in vec2 vLocalPx;
in vec2 vPosPx;
in vec2 vSize;

out vec4 FragColor;

uniform sampler2D uMask;
uniform sampler2D uBlur;
uniform vec2 uBlurScale;
uniform vec2 uBlurOffset;
uniform float uBlurAvailable;
uniform float uBlurPx;
uniform float uRimStrength;
uniform float uGlobalAlpha;
uniform float uShinePhase;
uniform vec4 uFresnel;
uniform float uFresnelPower;
uniform float uBaseAlpha;
uniform float uFresnelMix;
uniform float uDistort;
uniform vec4 uTint;
uniform vec4 uScissor;
uniform float uScissorEnabled;

const float TWO_PI = 6.2831853;
const float PI = 3.14159265;

float gaussMask(vec2 uv) {
    vec2 uvPerPx = vec2(1.0) / max(vSize, vec2(1.0));
    int r = clamp(int(ceil(uBlurPx)), 1, 7);
    float sigma = max(uBlurPx * 0.5, 0.001);
    float twoSigma2 = 2.0 * sigma * sigma;
    float sum = 0.0;
    float total = 0.0;
    for (int y = -r; y <= r; ++y) {
        for (int x = -r; x <= r; ++x) {
            float w = exp(-(float(x * x + y * y)) / twoSigma2);
            vec2 tap = uv + vec2(float(x), float(y)) * uvPerPx;
            sum += texture(uMask, clamp(tap, vec2(0.0), vec2(1.0))).a * w;
            total += w;
        }
    }
    return sum / max(total, 0.0001);
}

void main() {
    if (uScissorEnabled > 0.5) {
        if (vPosPx.x < uScissor.x || vPosPx.y < uScissor.y
            || vPosPx.x > uScissor.z || vPosPx.y > uScissor.w) {
            discard;
        }
    }

    vec2 uv = clamp(vLocalPx / max(vSize, vec2(1.0)), vec2(0.0), vec2(1.0));
    float mask = texture(uMask, uv).a;
    float blurred = gaussMask(uv);

    float edgeBand = 1.0 - clamp(abs(blurred - 0.5) * 2.5, 0.0, 1.0);
    float fresnel = pow(clamp(edgeBand, 0.0, 1.0), uFresnelPower);
    fresnel = clamp(fresnel, 0.0, 1.0);

    float halo = clamp((blurred - mask) * uRimStrength, 0.0, 1.0) * (1.0 - mask);

    vec2 stepUv = vec2(1.5, 1.5) / max(vSize, vec2(1.0));
    float gx = gaussMask(uv + vec2(stepUv.x, 0.0)) - gaussMask(uv - vec2(stepUv.x, 0.0));
    float gy = gaussMask(uv + vec2(0.0, stepUv.y)) - gaussMask(uv - vec2(0.0, stepUv.y));
    vec2 dir = (length(vec2(gx, gy)) > 0.001) ? normalize(vec2(-gx, -gy)) : vec2(0.0);

    vec2 blurUv = clamp(vPosPx * uBlurScale + uBlurOffset, 0.0, 1.0);
    vec4 scene = (uBlurAvailable > 0.5)
            ? texture(uBlur, blurUv + dir * uDistort)
            : vec4(mix(uTint.rgb, uFresnel.rgb, clamp(uTint.a, 0.0, 1.0)), 1.0);

    vec3 fillColor = mix(scene.rgb, uFresnel.rgb, fresnel * uFresnelMix);
    float fillAlpha = mask * mix(uBaseAlpha, 1.0, fresnel);

    float shine = 0.0;
    if (uShinePhase < 0.999) {
        vec2 dirToC = vLocalPx - vSize * 0.5;
        float ang = (length(dirToC) > 0.001) ? atan(dirToC.y, dirToC.x) : 0.0;
        float phase = uShinePhase * TWO_PI;
        float diff = abs(mod(ang - phase + PI, TWO_PI) - PI);
        float cycleFade = smoothstep(0.0, 0.12, uShinePhase) * (1.0 - smoothstep(0.88, 1.0, uShinePhase));
        float shineBand = max(edgeBand, halo);
        shine = smoothstep(0.95, 0.2, diff) * cycleFade * shineBand;
    }

    vec3 color = mix(fillColor, vec3(1.0), shine * 0.8);
    color = mix(color, vec3(1.0), halo * 0.5);

    float alpha = max(fillAlpha, max(halo, shine * 0.66)) * uGlobalAlpha;
    if (alpha < 0.001) {
        discard;
    }

    FragColor = vec4(color * alpha, alpha);
}
