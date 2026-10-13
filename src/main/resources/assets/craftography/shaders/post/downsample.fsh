#version 330
#extension GL_ARB_separate_shader_objects : require

// Craftography light meter: shrinks the picture for the histogram (four bilinear taps per output pixel).

#include <minecraft:globals.glsl>

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 q = 0.25 / OutSize;
    vec3 c = texture(InSampler, texCoord + vec2(-q.x, -q.y)).rgb + texture(InSampler, texCoord + vec2(q.x, -q.y)).rgb
        + texture(InSampler, texCoord + vec2(-q.x, q.y)).rgb + texture(InSampler, texCoord + vec2(q.x, q.y)).rgb;
    fragColor = vec4(c / 4.0, 1.0);
}
