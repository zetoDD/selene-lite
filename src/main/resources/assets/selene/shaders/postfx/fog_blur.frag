#version 330 core
layout(location = 0) out vec4 fragColor;

in vec2 vUv;

uniform sampler2D uSource;
uniform sampler2D uBlurred;
uniform sampler2D uDepth;
uniform vec2 uBlurredTexelSize;
uniform float uBlurResolutionScale;
uniform vec2 uNearFar;
uniform mat4 uInverseProjection;
uniform bool uInverseProjectionValid;
uniform vec2 uThreshold;
uniform vec3 uTintColor;
uniform float uTintStrength;

float linearizeDepth(float depthSample, float nearPlane, float farPlane) {
    float ndc = depthSample * 2.0 - 1.0;
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - ndc * (farPlane - nearPlane));
}

void main() {
    vec4 baseColor = texture(uSource, vUv);
    float scale = max(uBlurResolutionScale, 1e-6);
    vec2 normalizedTexel = uBlurredTexelSize * scale;
    vec2 clampedUv = clamp(vUv, normalizedTexel * 0.5, 1.0 - normalizedTexel * 0.5);
    vec3 blurred = texture(uBlurred, clampedUv).rgb;
    float depthSample = texture(uDepth, vUv).r;

    float nearPlane = max(1e-5, uNearFar.x);
    float farPlane = max(nearPlane + 1e-4, uNearFar.y);
    float thresholdMin = min(uThreshold.x, uThreshold.y);
    float thresholdMax = max(uThreshold.x, uThreshold.y);
    float range = max(1e-5, thresholdMax - thresholdMin);

    float viewDistance = linearizeDepth(depthSample, nearPlane, farPlane);
    if (uInverseProjectionValid) {
        vec2 ndc = vUv * 2.0 - 1.0;
        float clipZ = depthSample * 2.0 - 1.0;
        vec4 clip = vec4(ndc, clipZ, 1.0);
        vec4 view = uInverseProjection * clip;
        float w = abs(view.w);
        if (w > 1e-6) {
            vec3 viewPos = view.xyz / w;
            viewDistance = length(viewPos);
        }
    }

    float normalized = clamp((viewDistance - thresholdMin) / range, 0.0, 1.0);
    float fogWeight = normalized * normalized * (3.0 - 2.0 * normalized);

    vec3 finalColor = mix(baseColor.rgb, blurred, fogWeight);
    float tintAmount = clamp(uTintStrength, 0.0, 1.0) * fogWeight;
    vec3 tintedColor = mix(finalColor, uTintColor, tintAmount);
    fragColor = vec4(tintedColor, baseColor.a);
}
