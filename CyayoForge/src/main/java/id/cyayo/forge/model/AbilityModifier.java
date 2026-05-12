package id.cyayo.forge.model;

import java.util.HashMap;
import java.util.Map;

public class AbilityModifier {
    private final String abilityId;
    private final String trigger;
    private final Map<String, double[]> modifiers;

    public AbilityModifier(String abilityId, String trigger, Map<String, double[]> modifiers) {
        this.abilityId = abilityId;
        this.trigger = trigger;
        this.modifiers = modifiers;
    }

    public String getAbilityId() { return abilityId; }
    public String getTrigger() { return trigger; }
    public Map<String, double[]> getModifiers() { return modifiers; }

    public AbilityModifier mergeWith(AbilityModifier other) {
        Map<String, double[]> merged = new HashMap<>(this.modifiers);
        for (Map.Entry<String, double[]> entry : other.modifiers.entrySet()) {
            if (!merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue());
            } else {
                double[] current = merged.get(entry.getKey());
                double[] newVals = entry.getValue();
                // Merge dengan mengambil nilai terbesar
                merged.put(entry.getKey(), new double[]{
                    Math.max(current[0], newVals[0]),
                    Math.max(current[1], newVals[1])
                });
            }
        }
        return new AbilityModifier(abilityId, trigger, merged);
    }
}
