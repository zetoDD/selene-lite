#version 330 core
in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform float uOffset;

void main() {
    vec2 delta = uTexelSize * uOffset;
    vec2 dx = vec2(delta.x, 0.0);
    vec2 dy = vec2(0.0, delta.y);

    vec4 color = texture(uSource, vUv - dx - 2.0 * dy);
    color += texture(uSource, vUv - 2.0 * dx - dy);
    color += texture(uSource, vUv - 2.0 * dx + dy);
    color += texture(uSource, vUv - dx + 2.0 * dy);
    color += texture(uSource, vUv + dx + 2.0 * dy);
    color += texture(uSource, vUv + 2.0 * dx + dy);
    color += texture(uSource, vUv + 2.0 * dx - dy);
    color += texture(uSource, vUv + dx - 2.0 * dy);

    FragColor = color * 0.125;
}
