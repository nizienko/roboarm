export interface Point3D {
    x: number;
    y: number;
    z: number;
}

/** Meters. Length is measured between joint axes. */
export interface BeamDimensions {
    length: number;
    width: number;
    height: number;
}

export interface ArmPart {
    id: string;
    modelNode: string;
    position: Point3D;
    rotationDegreesZ: number;
    beam?: BeamDimensions | null;
    toggles: {
        name: string;
        value: number;
        targetValue: number;
        minValue: number;
        maxValue: number;
        unit: 'degrees' | 'meters';
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

export interface ArmTargetCommand {
    target: Point3D;
}

export type ArmRequest = ArmCommand | ArmTargetCommand;

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isFiniteNumber(value: unknown): value is number {
    return typeof value === 'number' && Number.isFinite(value);
}

function isPoint(value: unknown): value is Point3D {
    return isRecord(value) && [value.x, value.y, value.z].every(isFiniteNumber);
}

function isBeamDimensions(value: unknown): value is BeamDimensions {
    return isRecord(value) && [value.length, value.width, value.height]
        .every(dimension => isFiniteNumber(dimension) && dimension > 0);
}

export function isArmState(value: unknown): value is ArmState {
    return isRecord(value)
        && typeof value.time === 'number' && Number.isSafeInteger(value.time)
        && Array.isArray(value.parts)
        && value.parts.every((part: unknown) => isRecord(part)
            && typeof part.id === 'string'
            && typeof part.modelNode === 'string' && part.modelNode.length > 0
            && isPoint(part.position) && isFiniteNumber(part.rotationDegreesZ)
            && (part.beam === undefined || part.beam === null || isBeamDimensions(part.beam))
            && Array.isArray(part.toggles)
            && part.toggles.every((toggle: unknown) => isRecord(toggle)
                && typeof toggle.name === 'string'
                && (toggle.unit === 'degrees' || toggle.unit === 'meters')
                && isFiniteNumber(toggle.value)
                && isFiniteNumber(toggle.targetValue)
                && isFiniteNumber(toggle.minValue)
                && isFiniteNumber(toggle.maxValue)
                && toggle.minValue <= toggle.value
                && toggle.value <= toggle.maxValue
                && toggle.minValue <= toggle.targetValue
                && toggle.targetValue <= toggle.maxValue));
}
