#version 330 core
in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform float uRadius;

const float MIN_RADIUS = 0.05;
const float MAX_RADIUS = 30.0;
const float MIN_SIGMA = 0.02;
const int MAX_SAMPLES = 16;

void main() {
    float clampedRadius = clamp(uRadius, MIN_RADIUS, MAX_RADIUS);
    float sigma = max(clampedRadius * 0.5, MIN_SIGMA);
    float sigma2 = sigma * sigma;
    float twoSigma2 = 2.0 * sigma2;

    vec2 step = vec2(0.0, uTexelSize.y);
    
    float weights[MAX_SAMPLES + 1];
    weights[0] = 1.0;
    
    float stepFactor = exp(-1.0 / twoSigma2);
    float ratioFactor = exp(-1.0 / sigma2);
    float incremental = stepFactor;
    float current = weights[0];
    float total = weights[0];
    
    int samples = int(ceil(clampedRadius)) + 1;
    samples = min(samples, MAX_SAMPLES);
    
    for (int i = 0; i < samples; ++i) {
        current *= incremental;
        weights[i + 1] = current;
        total += 2.0 * current;
        incremental *= ratioFactor;
    }
    
    float normalization = 1.0 / total;
    
    vec4 color = texture(uSource, vUv) * weights[0] * normalization;
    
    for (int i = 1; i <= samples; ++i) {
        float weight = weights[i] * normalization;
        float offset = float(i);
        color += (texture(uSource, vUv + step * offset) + texture(uSource, vUv - step * offset)) * weight;
    }

    FragColor = color;
}
