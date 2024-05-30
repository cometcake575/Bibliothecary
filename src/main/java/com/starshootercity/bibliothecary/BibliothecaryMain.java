package com.starshootercity.bibliothecary;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BibliothecaryMain extends JavaPlugin {
    private static BibliothecaryMain instance;

    public static BibliothecaryMain getInstance() {
        return instance;
    }
    
    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(new LecternBookPlacer(), this);
        Bukkit.getPluginManager().registerEvents(new LibrarianLearner(), this);

        saveDefaultConfig();
    }
}