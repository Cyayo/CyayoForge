package id.cyayo.forge.model;

import java.util.List;

public class ForgeRecipe {
    private final String outputType;
    private final int minMaterialCount;
    private final ForgeType forgeType;
    private final String permission;
    private final List<String> regions;
    private final List<OutputTemplate> outputs; // sorted by base descending

    public ForgeRecipe(String outputType, int minMaterialCount, ForgeType forgeType,
                       String permission, List<String> regions, List<OutputTemplate> outputs) {
        this.outputType = outputType;
        this.minMaterialCount = minMaterialCount;
        this.forgeType = forgeType;
        this.permission = permission;
        this.regions = regions;
        this.outputs = outputs;
    }

    public String getOutputType()      { return outputType; }
    public int getMinMaterialCount()   { return minMaterialCount; }
    public ForgeType getForgeType()    { return forgeType; }
    public String getPermission()      { return permission; }
    public List<String> getRegions()   { return regions; }
    public List<OutputTemplate> getOutputs() { return outputs; }
}
