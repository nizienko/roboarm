import type { ArmState, Point3D } from './ArmState';

export type TargetBounds = Record<keyof Point3D, { min: number; max: number }>;

export function getTargetBounds(state?: ArmState): TargetBounds | undefined {
    const height = state?.parts.find(part => part.modelNode === 'Lift')?.toggles
        .find(toggle => toggle.unit === 'meters');
    const wrist = state?.parts.find(part => part.modelNode === 'Wrist');
    const reach = state?.parts.reduce((sum, part) => sum + (part.beam?.length ?? 0), 0) ?? 0;
    if (!height || !wrist || reach <= 0) return;

    // Telemetry positions are Y-up; target coordinates are Z-up.
    const offset = wrist.position.y - height.value;
    return {
        x: { min: -reach, max: reach },
        y: { min: -reach, max: reach },
        z: { min: height.minValue + offset, max: height.maxValue + offset },
    };
}

export function clampTarget(target: Point3D, bounds: TargetBounds): Point3D {
    const clamp = (axis: keyof Point3D) => Math.max(bounds[axis].min, Math.min(bounds[axis].max, target[axis]));
    return { x: clamp('x'), y: clamp('y'), z: clamp('z') };
}
