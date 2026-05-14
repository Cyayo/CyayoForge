package id.cyayo.forge.integration;

import dev.aurelium.auraskills.api.source.CustomSource;
import dev.aurelium.auraskills.api.source.SourceValues;

public class ForgingSource extends CustomSource {

    private final double multiplier;

    public ForgingSource(SourceValues values, double multiplier) {
        super(values);
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
