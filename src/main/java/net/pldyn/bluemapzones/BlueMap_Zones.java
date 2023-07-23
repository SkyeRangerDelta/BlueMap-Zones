package net.pldyn.bluemapzones;

import org.bukkit.plugin.java.JavaPlugin;

public final class BlueMap_Zones extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

        System.out.println("Zones plugin loaded!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        System.out.println("Zones plugin safely halted!");
    }
}
