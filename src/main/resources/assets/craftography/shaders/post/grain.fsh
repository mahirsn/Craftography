#version 330

// Craftography sensor noise: grows with ISO and is stronger in the shadows, like a real sensor.

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

layout(std140) uniform LensConfig {
    mat4 InvProjection;
    vec4 Exposure;
    vec4 Focus;
    vec4 Frame;
    vec4 Flags;
};

in vec2 texCoord;

out vec4 fragColor;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    if (Exposure.w > 0.0) {
        vec2 p = floor(texCoord * Frame.xy) + Frame.w * 17.0;
        // Sum of uniform noises: close to a normal distribution.
        float n = (hash(p) + hash(p + 41.7) + hash(p + 93.1) - 1.5);
        vec3 chroma = vec3(hash(p + 7.3), hash(p + 13.9), hash(p + 29.5)) - 0.5;
        float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
        float amount = Exposure.w * (1.2 - 0.6 * luma);
        c = clamp(c + amount * (n + 0.35 * chroma), 0.0, 1.0);
    }
    fragColor = vec4(c, 1.0);
}
