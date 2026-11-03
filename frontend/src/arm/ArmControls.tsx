import type { ArmCommand, ArmState } from "./ArmState";
import "./ArmControls.css";

interface ArmControlsProps {
    state?: ArmState;
    disabled: boolean;
    onCommand: (command: ArmCommand) => void;
}

export function ArmControls({ state, disabled, onCommand }: ArmControlsProps) {
    return (
        <fieldset className="arm-controls" disabled={disabled || !state}>
            <h4>Kinematics</h4>
            {state?.parts.map((part) => part.toggles.map((toggle) => {
                const id = `arm-control-${part.id}-${toggle.name}`;

                return (
                    <div className="arm-control" key={id}>
                        <label htmlFor={id}>
                            {toggle.name} ({toggle.unit === "meters" ? "m" : "°"})
                            <output htmlFor={id} title="Current position / target">
                                {toggle.value.toFixed(1)} / {toggle.targetValue.toFixed(1)}
                            </output>
                        </label>
                        <input
                            id={id}
                            type="range"
                            min={toggle.minValue}
                            max={toggle.maxValue}
                            step="any"
                            value={toggle.targetValue}
                            onChange={(event) => onCommand({
                                partId: part.id,
                                toggleName: toggle.name,
                                value: event.currentTarget.valueAsNumber,
                            })}
                        />
                        <div className="arm-control-limits" aria-hidden="true">
                            <span>{toggle.minValue}</span>
                            <span>{toggle.maxValue}</span>
                        </div>
                    </div>
                );
            }))}
        </fieldset>
    );
}
