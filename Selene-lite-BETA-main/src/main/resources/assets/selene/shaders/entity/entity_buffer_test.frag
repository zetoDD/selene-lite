#version 330 core
in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform vec4 uJumpDistance;

bool isValidSeed(vec2 seed) {
    return seed.x >= 0.0 && seed.y >= 0.0 && seed.x <= 1.0 && seed.y <= 1.0;
}

vec3 jump(vec3 minSeed, vec2 current, vec2 offset) {
    vec2 samplePos = current + offset;
    vec2 clamped = clamp(samplePos, vec2(0.0), vec2(1.0));
    if (any(notEqual(samplePos, clamped))) {
        return minSeed;
    }

    vec2 seed = texture(uSource, samplePos).xy;
    if (!isValidSeed(seed)) {
        return minSeed;
    }

    vec2 cScaled = floor(current * uTexelSize);
    vec2 sScaled = floor(seed * uTexelSize);
    float dist = distance(cScaled, sScaled);
    if (dist < minSeed.z) {
        return vec3(seed, dist);
    }
    return minSeed;
}

void main() {
    float jumpScalar = max(round(uJumpDistance.x), 1.0);
    vec2 jumpDist = jumpScalar / uTexelSize;

    vec3 curr = vec3(1.0, 1.0, 1.0e9);
    curr = jump(curr, vTexCoord, jumpDist * vec2(0.0, 0.0));
    curr = jump(curr, vTexCoord, jumpDist * vec2(0.0, 1.0));
    curr = jump(curr, vTexCoord, jumpDist * vec2(1.0, 1.0));
    curr = jump(curr, vTexCoord, jumpDist * vec2(1.0, 0.0));
    curr = jump(curr, vTexCoord, jumpDist * vec2(1.0, -1.0));
    curr = jump(curr, vTexCoord, jumpDist * vec2(0.0, -1.0));
    curr = jump(curr, vTexCoord, jumpDist * vec2(-1.0, -1.0));
    curr = jump(curr, vTexCoord, jumpDist * vec2(-1.0, 0.0));
    curr = jump(curr, vTexCoord, jumpDist * vec2(-1.0, 1.0));

    fragColor = vec4(curr.xy, 0.0, 1.0);
}
