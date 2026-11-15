import type { ArmTargetCommand, Point3D } from "./ArmState";
import type { TargetBounds } from "./TargetBounds";

interface IKArmControlsProps {
    target: Point3D;
    bounds?: TargetBounds;
    onTargetChange: (target: Point3D) => void;
    disabled: boolean;
    commandError?: string;
    onCommand: (command: ArmTargetCommand) => void;
}

export function IKArmControls({ target, bounds, onTargetChange, disabled, commandError, onCommand }: IKArmControlsProps) {
    const controlsDisabled = disabled || !bounds;

    return (
        <form className="arm-target" onSubmit={(event) => {
            event.preventDefault();
            if (!controlsDisabled && Object.values(target).every(Number.isFinite)) {
                onCommand({ target });
            }
        }}>
            <h4>Inverse Kinematics — Wrist position</h4>
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
                                min={bounds?.[axis].min}
                                max={bounds?.[axis].max}
                                value={target[axis]}
                                onChange={(event) => onTargetChange({ ...target, [axis]: event.currentTarget.valueAsNumber })}
                            />
                        </label>
                    ))}
                </div>
            </fieldset>
        </form>
    );
}
