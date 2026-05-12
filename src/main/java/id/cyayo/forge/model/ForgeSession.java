package id.cyayo.forge.model;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ForgeSession {
    private final UUID playerUUID;
    private final List<ItemStack> materials;
    private final ForgeRecipe recipe;
    private final ForgeTier resultTier;
    private final OutputTemplate selectedOutput;
    private final double finalMultiplier;
    private final List<AbilityModifier> injectedAbilities;
    private boolean injectSmith = true;

    // Minigame state
    private int currentRound = 0;
    private int totalPoints = 0;
    private ForgeQuality quality = ForgeQuality.GOOD;
    private final int totalRounds;
    private final int maxPointsPerRound;

    public ForgeSession(UUID playerUUID, List<ItemStack> materials, ForgeRecipe recipe,
                        ForgeTier resultTier, OutputTemplate selectedOutput,
                        double finalMultiplier, List<AbilityModifier> injectedAbilities,
                        int totalRounds, int maxPointsPerRound) {
        this.playerUUID = playerUUID;
        this.materials = materials;
        this.recipe = recipe;
        this.resultTier = resultTier;
        this.selectedOutput = selectedOutput;
        this.finalMultiplier = finalMultiplier;
        this.injectedAbilities = injectedAbilities;
        this.totalRounds = totalRounds;
        this.maxPointsPerRound = maxPointsPerRound;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public List<ItemStack> getMaterials() { return materials; }
    public ForgeRecipe getRecipe() { return recipe; }
    public ForgeTier getResultTier() { return resultTier; }
    public OutputTemplate getSelectedOutput() { return selectedOutput; }
    public double getFinalMultiplier() { return finalMultiplier; }
    public List<AbilityModifier> getInjectedAbilities() { return injectedAbilities; }

    public int getCurrentRound() { return currentRound; }
    public int getTotalPoints() { return totalPoints; }
    public ForgeQuality getQuality() { return quality; }
    public int getTotalRounds() { return totalRounds; }

    public void addPoints(int points) { totalPoints += points; }
    public void nextRound() { currentRound++; }
    public boolean isMinigameDone() { return currentRound >= totalRounds; }

    public void setQuality(ForgeQuality quality) { this.quality = quality; }

    public int getMaxPoints() {
        return totalRounds * maxPointsPerRound;
    }

    public double getPointPercentage() {
        int max = getMaxPoints();
        if (max <= 0) return 0;
        double percentage = (double) totalPoints / max * 100.0;
        return Math.max(0, percentage); // Clamp to 0
    }

    public boolean isInjectSmith() { return injectSmith; }
    public void setInjectSmith(boolean injectSmith) { this.injectSmith = injectSmith; }
}
