#version 330 core
layout(location = 0) out vec4 fragColor;

in vec2 vUv;

uniform sampler2D uSource;

void main() {
    fragColor = texture(uSource, vUv);
}
