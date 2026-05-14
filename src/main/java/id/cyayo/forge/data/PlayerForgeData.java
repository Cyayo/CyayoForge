package id.cyayo.forge.data;

import id.cyayo.forge.model.SalvageTask;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerForgeData {
    private final UUID uuid;
    private int total;
    private int weapon;
    private int armor;
    private final List<ItemStack> forgeHistory = new ArrayList<>();
    private final List<SalvageTask> salvageTasks = new ArrayList<>();

    public PlayerForgeData(UUID uuid) {
        this.uuid = uuid;
        this.total = 0;
        this.weapon = 0;
        this.armor = 0;
    }

    public PlayerForgeData(UUID uuid, int total, int weapon, int armor, List<ItemStack> history) {
        this.uuid = uuid;
        this.total = total;
        this.weapon = weapon;
        this.armor = armor;
        if (history != null) this.forgeHistory.addAll(history);
    }

    public UUID getUuid() { return uuid; }
    public int getTotal() { return total; }
    public int getWeapon() { return weapon; }
    public int getArmor() { return armor; }
    public List<ItemStack> getForgeHistory() { return forgeHistory; }
    
    public String getLastForgedName() {
        if (forgeHistory.isEmpty()) return "None";
        ItemStack last = forgeHistory.get(forgeHistory.size() - 1);
        if (last == null) return "None";
        return last.hasItemMeta() && last.getItemMeta().hasDisplayName() ? last.getItemMeta().getDisplayName() : last.getType().name();
    }

    public void incrementTotal() { total++; }
    public void incrementWeapon() { weapon++; }
    public void incrementArmor() { armor++; }
    public void addForgeHistory(ItemStack item, int maxHistory) {
        if (item == null) return;
        forgeHistory.add(item.clone());
        if (forgeHistory.size() > maxHistory) {
            forgeHistory.remove(0);
        }
    }

    public List<SalvageTask> getSalvageTasks() { return salvageTasks; }
    
    public void addSalvageTask(SalvageTask task) {
        salvageTasks.add(task);
    }
    
    public void removeSalvageTask(int slotIndex) {
        salvageTasks.removeIf(task -> task.getSlotIndex() == slotIndex);
    }
    
    public SalvageTask getSalvageTask(int slotIndex) {
        for (SalvageTask task : salvageTasks) {
            if (task.getSlotIndex() == slotIndex) return task;
        }
        return null;
    }
}
