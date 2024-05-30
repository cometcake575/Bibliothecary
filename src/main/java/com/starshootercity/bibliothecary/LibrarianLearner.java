package com.starshootercity.bibliothecary;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Lectern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LecternInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class LibrarianLearner implements Listener {
    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (event.getRecipe().getResult().getType() != Material.ENCHANTED_BOOK) return;
        Location location = event.getEntity().getMemory(MemoryKey.JOB_SITE);
        if (location != null) {
            if (location.getBlock().getState() instanceof Lectern lectern) {
                if (lectern.getInventory() instanceof LecternInventory inventory) {
                    if (inventory.getBook() != null) {
                        PersistentDataContainer pdc = inventory.getBook().getItemMeta().getPersistentDataContainer().get(LecternBookPlacer.enchantmentListKey, PersistentDataType.TAG_CONTAINER);
                        if (pdc != null) {
                            ItemStack item = LecternBookPlacer.getBook(pdc, null);
                            boolean go = true;
                            if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
                                if (meta.getStoredEnchants().size() > 1) {
                                    if (BibliothecaryMain.getInstance().getConfig().getBoolean("restrictions.disable-multi-enchants")) {
                                        go = false;
                                    }
                                }
                                for (Enchantment enchantment : meta.getStoredEnchants().keySet()) {
                                    if (!enchantment.isTradeable()) {
                                        if (BibliothecaryMain.getInstance().getConfig().getBoolean("restrictions.disable-non-villager-enchants")) {
                                            go = false;
                                        }
                                    }
                                }
                            }
                            if (go) {
                                inventory.setBook(null);
                                event.setRecipe(getMerchantRecipe(event, item));
                                return;
                            }
                        }
                    }
                }
            }
        }
        event.setCancelled(true);
    }

    private @NotNull MerchantRecipe getMerchantRecipe(VillagerAcquireTradeEvent event, ItemStack item) {
        MerchantRecipe recipe = event.getRecipe();
        MerchantRecipe newRecipe = new MerchantRecipe(
                item,
                recipe.getUses(),
                recipe.getMaxUses(),
                recipe.hasExperienceReward(),
                recipe.getVillagerExperience(),
                recipe.getPriceMultiplier(),
                recipe.getDemand(),
                recipe.getSpecialPrice(),
                recipe.shouldIgnoreDiscounts()
        );

        int amount = calculateEnchantmentCost(item);
        newRecipe.addIngredient(new ItemStack(Material.EMERALD, amount));
        newRecipe.addIngredient(new ItemStack(Material.BOOK));
        return newRecipe;
    }

    private final Random random = new Random();

    public int calculateEnchantmentCost(ItemStack item) {
        int i = 0;
        if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            for (Enchantment enchantment : meta.getStoredEnchants().keySet()) {
                int cost = calculateEnchantmentCost(enchantment, meta.getStoredEnchantLevel(enchantment));
                i += cost;
            }
        }
        return Math.min(i, 64);
    }

    public int calculateEnchantmentCost(Enchantment enchantment, int level) {
        int cost = 2 + random.nextInt(5 + level * 10) + 3 * level;
        if (enchantment.isTreasure()) {
            cost *= 2;
        }
        return Math.min(cost, 64);
    }
}
