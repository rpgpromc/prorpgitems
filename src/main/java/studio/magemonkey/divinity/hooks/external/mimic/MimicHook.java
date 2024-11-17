package studio.magemonkey.divinity.hooks.external.mimic;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import ru.endlesscode.mimic.MimicApiLevel;
import ru.endlesscode.mimic.items.BukkitItemsRegistry;
import studio.magemonkey.divinity.Divinity;

public class MimicHook {

    private static Divinity divinity;

    public static void hook(Divinity divinity) {
        if (Bukkit.getPluginManager().getPlugin("Mimic") == null) return;
        if (!MimicApiLevel.checkApiLevel(MimicApiLevel.VERSION_0_6)) {
            divinity.getLogger().severe(
                    "At least Mimic 0.6.1 is required. " +
                            "Please download it from https://www.spigotmc.org/resources/82515/"
            );
            return;
        }

        MimicHook.divinity = divinity;
        try {
            registerMimicServices();
        } catch (Exception ex) {
            divinity.getLogger().severe("Mimic hook failed: " + ex.getLocalizedMessage());
        }
    }

    private static void registerMimicServices() {
        //registerService(BukkitLevelSystem.Provider.class, new DivinityLevelSystem.Provider());
        //registerService(BukkitClassSystem.Provider.class, new DivinityClassSystem.Provider());
        registerService(BukkitItemsRegistry.class, new DivinityItemsRegistry(divinity));
    }

    private static <T> void registerService(Class<T> aClass, T service) {
        Bukkit.getServicesManager()
                .register(aClass, service, divinity, ServicePriority.High);
    }
}
