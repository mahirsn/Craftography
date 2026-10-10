#version 330

// Craftography lens: depth of field, exposure, white balance and shutter-speed motion blur.
// Reads the finished frame (Scene), the level depth and the previous output (History).

#moj_import <minecraft:globals.glsl>

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D HistorySampler;

layout(std140) uniform LensConfig {
    mat4 InvProjection;
    vec4 Exposure; // rgb: white balance times exposure gain (linear light), w: grain amplitude
    vec4 Focus;    // x: blur diameter in px per |1/S - 1/d| (1/m), y: 1/S, z: largest blur radius in px, w: depth of field on
    vec4 Frame;    // xy: size in px, z: weight of the previous frame, w: grain seed
    vec4 Flags;    // x: depth range 0..1 (1) or -1..1 (0)
};

in vec2 texCoord;

out vec4 fragColor;

const int TAPS = 64;
const float GOLDEN_ANGLE = 2.39996323;

float viewDistance(vec2 uv) {
    float d = texture(DepthSampler, uv).r;
    // Reversed depth: 0 is the far plane (sky).
    if (d <= 0.0) return 1e6;
    float z = Flags.x > 0.5 ? d : d * 2.0 - 1.0;
    vec4 v = InvProjection * vec4(uv * 2.0 - 1.0, z, 1.0);
    return max(-v.z / v.w, 0.01);
}

float blurRadius(float distance) {
    return min(0.5 * Focus.x * abs(Focus.y - 1.0 / distance), Focus.z);
}

vec3 depthOfField(vec2 uv) {
    vec3 centre = texture(SceneSampler, uv).rgb;
    if (Focus.w < 0.5 || Focus.z < 0.5) return centre;
    float centreDistance = viewDistance(uv);
    float centreRadius = blurRadius(centreDistance);
    vec2 texel = 1.0 / Frame.xy;
    vec3 sum = centre;
    float weights = 1.0;
    // Gather over the largest blur radius: a sample counts when its own blur disc reaches this pixel.
    for (int i = 0; i < TAPS; i++) {
        float r = sqrt((float(i) + 0.5) / float(TAPS)) * Focus.z;
        float a = float(i) * GOLDEN_ANGLE;
        vec2 suv = uv + vec2(cos(a), sin(a)) * r * texel;
        float sDistance = viewDistance(suv);
        float sRadius = blurRadius(sDistance);
        // Something behind this pixel cannot blur over it further than this pixel is blurred itself.
        if (sDistance > centreDistance) sRadius = min(sRadius, centreRadius);
        float w = clamp(sRadius - r + 1.0, 0.0, 1.0);
        sum += texture(SceneSampler, suv).rgb * w;
        weights += w;
    }
    return sum / weights;
}

vec3 toLinear(vec3 c) {
    return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(0.04045, c));
}

vec3 toSrgb(vec3 c) {
    return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, c));
}

void main() {
    vec3 c = toLinear(depthOfField(texCoord));
    // Exposure and white balance act on light, so they work in linear space; the sensor clips at full white.
    c = clamp(c * Exposure.rgb, 0.0, 1.0);
    vec3 outColor = toSrgb(c);
    // A slow shutter keeps collecting light while the picture moves.
    outColor = mix(outColor, texture(HistorySampler, texCoord).rgb, Frame.z);
    fragColor = vec4(outColor, 1.0);
}
