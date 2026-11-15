import { useState } from "react";
import type { ArmState, ArmTargetCommand, Point3D } from "./ArmState";
import { clampTarget, getTargetBounds } from "./TargetBounds";

interface IKArmControlsProps {
    state?: ArmState;
    disabled: boolean;
    commandError?: string;
    onCommand: (command: ArmTargetCommand) => void;
}

export function IKArmControls({ state, disabled, commandError, onCommand }: IKArmControlsProps) {
    const [target, setTarget] = useState<Point3D>({ x: 2, y: 0, z: 1.5 });
    const bounds = getTargetBounds(state);
    const controlsDisabled = disabled || !bounds;
    const boundedTarget = bounds ? clampTarget(target, bounds) : target;

    return (
        <form className="arm-target" onSubmit={(event) => {
            event.preventDefault();
            if (!controlsDisabled && Object.values(boundedTarget).every(Number.isFinite)) {
                onCommand({ target: boundedTarget });
            }
        }}>
            <h4>Inverse Kinematics — Wrist position</h4>
            <fieldset disabled={controlsDisabled}>
                <p>x={boundedTarget.x.toFixed(2)}; y={boundedTarget.y.toFixed(2)}; z={boundedTarget.z.toFixed(2)}</p>
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
                                min={bounds?.[axis].min}
                                max={bounds?.[axis].max}
                                value={boundedTarget[axis]}
                                onChange={(event) => setTarget({ ...boundedTarget, [axis]: event.currentTarget.valueAsNumber })}
                            />
                        </label>
                    ))}
                </div>
            </fieldset>
        </form>
    );
}
