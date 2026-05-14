package id.cyayo.forge.integration;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedRegistry;
import dev.aurelium.auraskills.api.skill.CustomSkill;
import dev.aurelium.auraskills.api.source.SourceType;
import dev.aurelium.auraskills.api.source.XpSourceParser;
import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.api.event.ForgeSuccessEvent;
import id.cyayo.forge.model.ForgeQuality;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import dev.aurelium.auraskills.api.registry.NamespacedId;

public class AuraSkillsIntegration implements Listener {

    private final CyayoForge plugin;
    private final AuraSkillsApi auraSkills;
    private CustomSkill forgingSkill;
    private SourceType forgingSourceType;

    public AuraSkillsIntegration(CyayoForge plugin) {
        this.plugin = plugin;
        this.auraSkills = AuraSkillsApi.get();
    }

    public void register() {
        NamespacedRegistry registry = auraSkills.useRegistry("cyayoforge", plugin.getDataFolder());

        // Register Custom Skill
        this.forgingSkill = CustomSkill.builder(NamespacedId.of("cyayoforge", "forging"))
                .displayName("Forging")
                .description("Tempa item untuk mendapatkan Forging XP")
                .build();
        registry.registerSkill(forgingSkill);

        // Register Custom Ability: Master Forger
        dev.aurelium.auraskills.api.ability.CustomAbility masterForger = dev.aurelium.auraskills.api.ability.CustomAbility.builder(NamespacedId.of("cyayoforge", "master_forger"))
                .displayName("Master Forger")
                .description("Meningkatkan XP yang didapatkan saat menempa sebesar {value}%.")
                .baseValue(10.0)
                .valuePerLevel(5.0)
                .unlock(5)
                .levelUp(5)
                .build();
        registry.registerAbility(masterForger);

        // Register Source Type
        this.forgingSourceType = registry.registerSourceType("forging", (XpSourceParser<ForgingSource>) (source, context) -> {
            double multiplier = source.node("multiplier").getDouble(1.0);
            return new ForgingSource(context.parseValues(source), multiplier);
        });

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("AuraSkills integration registered!");
    }

    @EventHandler
    public void onForgeSuccess(ForgeSuccessEvent event) {
        id.cyayo.forge.model.ForgeQuality quality = event.getSession().getQuality();
        double xpAmount = calculateXp(quality);

        dev.aurelium.auraskills.api.user.SkillsUser user = auraSkills.getUser(event.getPlayer().getUniqueId());
        
        // Apply Master Forger bonus if unlocked
        dev.aurelium.auraskills.api.ability.Ability masterForger = auraSkills.getGlobalRegistry().getAbility(NamespacedId.of("cyayoforge", "master_forger"));
        double bonus = 0;
        if (masterForger != null) {
            int level = user.getAbilityLevel(masterForger);
            if (level > 0) {
                bonus = masterForger.getValue(level);
            }
        }
        
        double finalXp = xpAmount * (1.0 + (bonus / 100.0));

        user.addSkillXp(forgingSkill, finalXp);
    }

    private double calculateXp(ForgeQuality quality) {
        return switch (quality) {
            case PERFECT -> 250.0;
            case EXQUISITE -> 100.0;
            case GOOD -> 50.0;
            case FLAWED -> 25.0;
            case BROKEN -> 10.0;
        };
    }
}
