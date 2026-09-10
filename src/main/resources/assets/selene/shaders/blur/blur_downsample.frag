#version 330 core
in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform float uOffset;

void main() {
    vec2 delta = uTexelSize * uOffset;

    vec4 color = texture(uSource, vUv + vec2(-delta.x, -delta.y));
    color += texture(uSource, vUv + vec2(delta.x, -delta.y));
    color += texture(uSource, vUv + vec2(-delta.x, delta.y));
    color += texture(uSource, vUv + vec2(delta.x, delta.y));

    FragColor = color * 0.25;
}
