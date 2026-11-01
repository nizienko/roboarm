export interface Point3D {
    x: number;
    y: number;
    z: number;
}

export interface ArmPart {
    id: string;
    mesh: {
        startPoint: Point3D;
        endPoint: Point3D;
    };
    toggles: {
        name: string;
        value: number;
        minValue: number;
        maxValue: number;
    }[];
}

export interface ArmState {
    time: number;
    parts: ArmPart[];
}

export interface ArmCommand {
    partId: string;
    toggleName: string;
    value: number;
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isFiniteNumber(value: unknown): value is number {
    return typeof value === "number" && Number.isFinite(value);
}

function isPoint(value: unknown): value is Point3D {
    return isRecord(value) && [value.x, value.y, value.z].every(isFiniteNumber);
}

export function isArmState(value: unknown): value is ArmState {
    return isRecord(value)
        && typeof value.time === "number" && Number.isSafeInteger(value.time)
        && Array.isArray(value.parts)
        && value.parts.every((part: unknown) => isRecord(part)
            && typeof part.id === "string"
            && isRecord(part.mesh)
            && isPoint(part.mesh.startPoint) && isPoint(part.mesh.endPoint)
            && Array.isArray(part.toggles)
            && part.toggles.every((toggle: unknown) => isRecord(toggle)
                && typeof toggle.name === "string"
                && isFiniteNumber(toggle.value)
                && isFiniteNumber(toggle.minValue)
                && isFiniteNumber(toggle.maxValue)
                && toggle.minValue <= toggle.value
                && toggle.value <= toggle.maxValue));
}
