#version 430 core

struct InstanceData {
    vec4 corner0;
    vec4 corner1;
    ivec4 clip;
    vec4 clipRadii;
    vec2 pos;
    vec2 size;
    uvec4 colors;
    vec4 radii;
    vec2 uv0;
    vec2 uv1;
    uint flags;
    int texSlot;
    int pad0;
    int pad1;
};

layout(std430, binding = 0) readonly buffer Instances {
    InstanceData instances[];
};

uniform vec2 uViewport;

out vec2 vLocalPx;
out vec2 vSize;
flat out vec4 vColorTL;
flat out vec4 vColorTR;
flat out vec4 vColorBR;
flat out vec4 vColorBL;
flat out vec4 vRadii;
flat out uint vFlags;
flat out ivec4 vClip;
flat out vec4 vClipRadii;
out vec2 vUV;
flat out vec2 vUvScale;
flat out vec2 vUvOffset;
flat out int vTexSlot;
out vec2 vPosPx;

const vec2 QUAD[6] = vec2[6](
vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 1.0)
);

void main(){
    int iid = gl_VertexID / 6;
    int vid = gl_VertexID % 6;

    if (iid >= instances.length()) {
        gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    InstanceData inst = instances[iid];

    vec2 local = QUAD[vid];
    vSize = inst.size;
    vLocalPx = local * inst.size;

    vColorTL = unpackUnorm4x8(inst.colors.x);
    vColorTR = unpackUnorm4x8(inst.colors.y);
    vColorBR = unpackUnorm4x8(inst.colors.z);
    vColorBL = unpackUnorm4x8(inst.colors.w);
    vRadii = inst.radii;
    vFlags = inst.flags;
    vClip = inst.clip;
    vClipRadii = inst.clipRadii;
    vTexSlot = inst.texSlot;
    vUV = mix(inst.uv0, inst.uv1, local);
    vUvScale = inst.uv0;
    vUvOffset = inst.uv1;

    vec2 posTL = inst.corner0.xy;
    vec2 posTR = inst.corner0.zw;
    vec2 posBR = inst.corner1.xy;
    vec2 posBL = inst.corner1.zw;

    vec2 top = mix(posTL, posTR, local.x);
    vec2 bottom = mix(posBL, posBR, local.x);
    vec2 posPx = mix(top, bottom, local.y);
    vPosPx = posPx;

    vec2 ndc = vec2((posPx.x / uViewport.x) * 2.0 - 1.0,
                    1.0 - (posPx.y / uViewport.y) * 2.0);
    gl_Position = vec4(ndc, 0.0, 1.0);
}