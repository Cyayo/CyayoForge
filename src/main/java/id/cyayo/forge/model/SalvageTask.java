package id.cyayo.forge.model;

import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;

public class SalvageTask {
    private final int slotIndex;
    private final ItemStack originalItem;
    private final List<Map<String, Object>> materials;
    private final long startTime;
    private final long endTime;
    private boolean completed;

    public SalvageTask(int slotIndex, ItemStack originalItem, List<Map<String, Object>> materials, long startTime, long endTime) {
        this.slotIndex = slotIndex;
        this.originalItem = originalItem;
        this.materials = materials;
        this.startTime = startTime;
        this.endTime = endTime;
        this.completed = false;
    }

    public int getSlotIndex() { return slotIndex; }
    public ItemStack getOriginalItem() { return originalItem; }
    public List<Map<String, Object>> getMaterials() { return materials; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public long getTimeLeft() {
        long left = (endTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, left);
    }

    public boolean isFinished() {
        return System.currentTimeMillis() >= endTime;
    }
}
