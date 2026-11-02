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
            {state?.parts.map((part) => part.toggles.map((toggle) => {
                const id = `arm-control-${part.id}-${toggle.name}`;

                return (
                    <div className="arm-control" key={id}>
                        <label htmlFor={id}>
                            {toggle.name}
                            <output htmlFor={id} title="Current position / target">
                                {Number(toggle.value.toFixed(2))} / {Number(toggle.targetValue.toFixed(2))}
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
