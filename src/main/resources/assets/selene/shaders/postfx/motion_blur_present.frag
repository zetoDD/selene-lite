#version 330 core

// Straight copy of the accumulated motion-blur layer back onto the frame that
// the GUI would otherwise have drawn into. The source is premultiplied alpha,
// so the caller blends with (ONE, ONE_MINUS_SRC_ALPHA).

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uSource;

void main() {
    FragColor = texture(uSource, vUv);
}
