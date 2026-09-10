#version 330 core


layout(location = 0) in vec2 aLocal;

uniform vec2 uViewport;
uniform vec4 uRect;

out vec2 vLocalPx;
out vec2 vPosPx;
out vec2 vSize;

const vec2 QUAD[6] = vec2[6](
    vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
    vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 1.0)
);

void main() {
    vec2 size = uRect.zw;
    vSize = size;
    vLocalPx = aLocal * size;
    vPosPx = uRect.xy + vLocalPx;

    vec2 ndc = vec2(
        (vPosPx.x / uViewport.x) * 2.0 - 1.0,
        1.0 - (vPosPx.y / uViewport.y) * 2.0
    );
    gl_Position = vec4(ndc, 0.0, 1.0);
}
