#version 330
uniform sampler2D InSampler;
uniform sampler2D OriginalSampler;
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
    vec4 LcdBounds;
};
in vec2 texCoord;
out vec4 fragColor;
void main() {
    bool inside = texCoord.x >= LcdBounds.x && texCoord.x < LcdBounds.z
        && texCoord.y >= LcdBounds.y && texCoord.y < LcdBounds.w;
    fragColor = inside ? texture(InSampler, texCoord) : texture(OriginalSampler, texCoord);
}
