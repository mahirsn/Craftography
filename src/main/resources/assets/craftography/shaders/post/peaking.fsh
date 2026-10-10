#version 330

// Craftography focus peaking: marks strong local contrast (what is in focus) in red. Live view only.

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

float luma(vec2 uv) {
    return dot(texture(InSampler, uv).rgb, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec2 t = 1.0 / InSize;
    float gx = luma(texCoord + vec2(t.x, 0.0)) - luma(texCoord - vec2(t.x, 0.0));
    float gy = luma(texCoord + vec2(0.0, t.y)) - luma(texCoord - vec2(0.0, t.y));
    vec3 c = texture(InSampler, texCoord).rgb;
    float edge = smoothstep(0.16, 0.26, length(vec2(gx, gy)));
    fragColor = vec4(mix(c, vec3(1.0, 0.15, 0.1), edge), 1.0);
}
