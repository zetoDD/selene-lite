#version 330 core








in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uCurrent;
uniform sampler2D uHistory;


uniform vec2 uVelocity;

uniform float uMix;

uniform int uSamples;

uniform float uChroma;

const int MAX_SAMPLES = 12;

void main() {
    vec4 current;
    int samples = clamp(uSamples, 1, MAX_SAMPLES);

    if (samples > 1) {
        vec4 acc = texture(uCurrent, vUv);
        float total = 1.0;
        bool chroma = uChroma > 0.0005;

        for (int i = 1; i < MAX_SAMPLES; ++i) {
            if (i >= samples) {
                break;
            }

            float t = float(i) / float(samples);
            float weight = 1.0 - 0.7 * t;
            vec2 base = uVelocity * t;

            if (chroma) {
                vec4 sR = texture(uCurrent, vUv - base * (1.0 + uChroma));
                vec4 sG = texture(uCurrent, vUv - base);
                vec4 sB = texture(uCurrent, vUv - base * (1.0 - uChroma));
                acc += vec4(sR.r, sG.g, sB.b, sG.a) * weight;
            } else {
                acc += texture(uCurrent, vUv - base) * weight;
            }

            total += weight;
        }

        current = acc / total;
    } else {
        current = texture(uCurrent, vUv);
    }

    vec4 history = texture(uHistory, vUv);
    FragColor = mix(history, current, clamp(uMix, 0.0, 1.0));
}
