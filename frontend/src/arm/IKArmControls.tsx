import { useState } from "react";
import type { ArmState, ArmTargetCommand, Point3D } from "./ArmState";

interface IKArmControlsProps {
    state?: ArmState;
    disabled: boolean;
    commandError?: string;
    onCommand: (command: ArmTargetCommand) => void;
}

export function IKArmControls({ state, disabled, commandError, onCommand }: IKArmControlsProps) {
    const [target, setTarget] = useState<Point3D>({ x: 2, y: 0, z: 1.5 });
    const height = state?.parts.find((part) => part.modelNode === "Lift")?.toggles
        .find((toggle) => toggle.name === "Height");
    const gripperJaw = state?.parts.find((part) => part.modelNode === "GripperJaw");
    const heightOffset = height && gripperJaw ? gripperJaw.position.y - height.value : undefined;
    const controlsDisabled = disabled || heightOffset === undefined;
    const minZ = height && heightOffset !== undefined ? Number((height.minValue + heightOffset).toFixed(10)) : undefined;
    const maxZ = height && heightOffset !== undefined ? Number((height.maxValue + heightOffset).toFixed(10)) : undefined;

    return (
        <form className="arm-target" onSubmit={(event) => {
            event.preventDefault();
            if (!controlsDisabled && Object.values(target).every(Number.isFinite)) {
                onCommand({ target });
            }
        }}>
            <h4>Inverse Kinematics</h4>
            <fieldset disabled={controlsDisabled}>
                <p>x={target.x.toFixed(2)}; y={target.y.toFixed(2)}; z={target.z.toFixed(2)}</p>
                <button type="submit">Send command</button>
                <div role="alert">{commandError}</div>
                <div className="arm-target-coordinates">
                    {(["x", "y", "z"] as const).map((axis) => (
                        <label key={axis} htmlFor={`arm-target-${axis}`}>
                            {axis.toUpperCase()} (m)
                            <input
                                id={`arm-target-${axis}`}
                                type="range"
                                step="any"
                                min={axis === "z" ? minZ : -2}
                                max={axis === "z" ? maxZ : 2}
                                value={target[axis]}
                                onChange={(event) => setTarget({ ...target, [axis]: event.currentTarget.valueAsNumber })}
                            />
                        </label>
                    ))}
                </div>
            </fieldset>
        </form>
    );
}
