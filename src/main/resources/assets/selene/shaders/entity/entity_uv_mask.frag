#version 330 core
in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uSource;
uniform float uAlphaThreshold;

void main() {
    vec4 sampleColor = texture(uSource, vTexCoord);
    float alpha = sampleColor.a;
    if (alpha > uAlphaThreshold) {
        fragColor = vec4(vTexCoord, 0.0, 1.0);
    } else {
        fragColor = vec4(-1.0, -1.0, 0.0, -1.0);
    }
}
