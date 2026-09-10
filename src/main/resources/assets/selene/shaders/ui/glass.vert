#version 330 core
layout(location = 0) in vec2 aLocal;
layout(location = 1) in vec4 aRect;
layout(location = 2) in vec4 aRadii;
layout(location = 3) in vec4 aAlphaPowerMix;
layout(location = 4) in vec4 aFresnel;
layout(location = 5) in vec4 aFlags;
layout(location = 6) in vec4 aTint;

uniform vec2 uViewport;

out vec2 vLocalPx;
out vec2 vPosPx;
flat out vec2 vSize;
flat out vec4 vRadii;
flat out vec4 vAlphaPowerMix;
flat out vec4 vFresnel;
flat out vec4 vFlags;
flat out vec4 vTint;

const vec2 QUAD[6] = vec2[6](
    vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
    vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 1.0)
);

void main() {
    vec2 size = aRect.zw;
    vSize = size;
    vLocalPx = aLocal * size;
    vPosPx = aRect.xy + vLocalPx;
    vRadii = aRadii;
    vAlphaPowerMix = aAlphaPowerMix;
    vFresnel = aFresnel;
    vFlags = aFlags;
    vTint = aTint;

    vec2 ndc = vec2(
        (vPosPx.x / uViewport.x) * 2.0 - 1.0,
        1.0 - (vPosPx.y / uViewport.y) * 2.0
    );
    gl_Position = vec4(ndc, 0.0, 1.0);
}
