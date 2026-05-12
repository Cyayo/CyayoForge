package id.cyayo.forge.data;

import id.cyayo.forge.model.SalvageTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerForgeData {
    private final UUID uuid;
    private int total;
    private int weapon;
    private int armor;
    private String lastForged;
    private final List<SalvageTask> salvageTasks = new ArrayList<>();

    public PlayerForgeData(UUID uuid) {
        this.uuid = uuid;
        this.total = 0;
        this.weapon = 0;
        this.armor = 0;
        this.lastForged = "None";
    }

    public PlayerForgeData(UUID uuid, int total, int weapon, int armor, String lastForged) {
        this.uuid = uuid;
        this.total = total;
        this.weapon = weapon;
        this.armor = armor;
        this.lastForged = lastForged;
    }

    public UUID getUuid() { return uuid; }
    public int getTotal() { return total; }
    public int getWeapon() { return weapon; }
    public int getArmor() { return armor; }
    public String getLastForged() { return lastForged; }

    public void incrementTotal() { total++; }
    public void incrementWeapon() { weapon++; }
    public void incrementArmor() { armor++; }
    public void setLastForged(String lastForged) { this.lastForged = lastForged; }

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
