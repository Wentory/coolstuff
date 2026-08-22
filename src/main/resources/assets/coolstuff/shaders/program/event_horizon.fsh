#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Strength;
uniform float Impact;
uniform float SpawnImpact;
uniform float Danger;
uniform float LensX;
uniform float LensY;
uniform float LensStrength;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 screenCenter = vec2(0.5);
    vec2 screenAspect = vec2(OutSize.x / max(OutSize.y, 1.0), 1.0);
    vec2 screenDelta = (texCoord - screenCenter) * screenAspect;
    float screenRadius = length(screenDelta);
    float fisheye = SpawnImpact * 0.62 * max(0.0, 1.0 - screenRadius * screenRadius * 0.72);
    vec2 birthWarpedUv = screenCenter + (texCoord - screenCenter) * (1.0 - fisheye);

    vec2 lensCenter = vec2(LensX, LensY);
    vec2 lensAspect = vec2(OutSize.x / max(OutSize.y, 1.0), 1.0);
    vec2 lensDelta = (birthWarpedUv - lensCenter) * lensAspect;
    float lensDistance = length(lensDelta);
    vec2 lensDirection = lensDistance > 0.0001 ? normalize(lensDelta) / lensAspect : vec2(0.0);
    float lensMask = smoothstep(0.015, 0.09, lensDistance) * (1.0 - smoothstep(0.10, 0.42, lensDistance));
    float lensBend = lensMask * LensStrength * 0.055;
    vec2 lensedUv = birthWarpedUv + lensDirection * lensBend;

    vec2 center = vec2(0.5);
    vec2 aspect = vec2(OutSize.x / max(OutSize.y, 1.0), 1.0);
    vec2 fromCenter = (lensedUv - center) * aspect;
    float distanceFromCenter = length(fromCenter);
    vec2 radial = distanceFromCenter > 0.0001 ? normalize(fromCenter) / aspect : vec2(0.0);

    float ripple = sin(distanceFromCenter * 42.0) * 0.0028 * Strength;
    vec2 warpedUv = lensedUv + radial * ripple;
    vec2 blurStep = radial * (0.0038 * Strength + 0.0045 * Danger + 0.012 * Impact + 0.038 * SpawnImpact);

    vec3 blurred = vec3(0.0);
    blurred += texture(DiffuseSampler, warpedUv - blurStep * 3.0).rgb * 0.08;
    blurred += texture(DiffuseSampler, warpedUv - blurStep * 2.0).rgb * 0.12;
    blurred += texture(DiffuseSampler, warpedUv - blurStep).rgb * 0.18;
    blurred += texture(DiffuseSampler, warpedUv).rgb * 0.24;
    blurred += texture(DiffuseSampler, warpedUv + blurStep).rgb * 0.18;
    blurred += texture(DiffuseSampler, warpedUv + blurStep * 2.0).rgb * 0.12;
    blurred += texture(DiffuseSampler, warpedUv + blurStep * 3.0).rgb * 0.08;

    vec2 chroma = radial * (0.008 * Strength + 0.018 * Impact + 0.026 * SpawnImpact);
    blurred.r = texture(DiffuseSampler, warpedUv + chroma).r;
    blurred.b = texture(DiffuseSampler, warpedUv - chroma).b;

    float edgePulse = smoothstep(0.15, 0.75, distanceFromCenter) * Strength;
    blurred += vec3(0.03, 0.08, 0.13) * edgePulse;
    float dangerVignette = smoothstep(0.16, 0.78, distanceFromCenter) * Danger;
    blurred *= 1.0 - dangerVignette * 0.32;
    blurred += vec3(0.02, 0.12, 0.20) * dangerVignette;
    blurred += vec3(0.45, 0.70, 1.0) * Impact * Impact * 0.22;
    blurred += vec3(0.28, 0.52, 0.82) * SpawnImpact * SpawnImpact * 0.12;
    fragColor = vec4(blurred, 1.0);
}
