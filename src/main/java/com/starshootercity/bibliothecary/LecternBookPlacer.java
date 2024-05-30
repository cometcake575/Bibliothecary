package com.starshootercity.bibliothecary;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Lectern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LecternInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LecternBookPlacer implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (event.getClickedBlock() == null) return;
        if (event.getItem() == null) return;
        EquipmentSlot slot = Objects.requireNonNullElse(event.getHand(), EquipmentSlot.HAND);
        if (!(event.getItem().getItemMeta() instanceof EnchantmentStorageMeta esm)) return;
        if (event.getClickedBlock().getState() instanceof Lectern lectern) {
            if (lectern.getInventory() instanceof LecternInventory inventory) {
                if (inventory.getBook() != null) return;
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                ItemMeta meta = book.getItemMeta();
                PersistentDataContainer pdc = meta.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
                for (Enchantment enchantment : esm.getStoredEnchants().keySet()) {
                    pdc.set(enchantment.getKey(), PersistentDataType.INTEGER, esm.getStoredEnchants().get(enchantment));
                }
                meta.getPersistentDataContainer().set(enchantmentListKey, PersistentDataType.TAG_CONTAINER, pdc);
                meta.displayName(event.getItem().getItemMeta().displayName());
                book.setItemMeta(meta);
                inventory.setBook(book);
                event.getPlayer().swingHand(slot);
                event.getItem().setAmount(event.getItem().getAmount() - 1);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory() instanceof LecternInventory inventory) {
            if (inventory.getBook() == null) return;
            ItemMeta meta = inventory.getBook().getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer().get(enchantmentListKey, PersistentDataType.TAG_CONTAINER);
            if (pdc == null) return;
            event.setCancelled(true);
            inventory.setBook(null);
            ItemStack book = getBook(pdc, meta.displayName());
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR) {
                event.getPlayer().getInventory().setItemInMainHand(book);
            } else for (ItemStack item : event.getPlayer().getInventory().addItem(book).values()) {
                event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), item);
            }
        }
    }

    public static final NamespacedKey enchantmentListKey = new NamespacedKey(BibliothecaryMain.getInstance(), "enchantments");
    private static Map<NamespacedKey, Enchantment> keyEnchantmentMap;

    @SuppressWarnings("deprecation")
    public LecternBookPlacer() {
        keyEnchantmentMap = new HashMap<>();
        for (Enchantment enchantment : Enchantment.values()) {
            keyEnchantmentMap.put(enchantment.getKey(), enchantment);
        }
    }

    public static ItemStack getBook(PersistentDataContainer pdc, @Nullable Component displayName) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        if (bookMeta instanceof EnchantmentStorageMeta storageMeta) {
            for (NamespacedKey key : pdc.getKeys()) {
                Enchantment enchantment = keyEnchantmentMap.get(key);
                if (enchantment == null) continue;
                int level = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
                if (level == 0) continue;
                storageMeta.addStoredEnchant(enchantment, level, true);
            }
        }
        if (displayName != null) bookMeta.displayName(displayName);
        book.setItemMeta(bookMeta);
        return book;
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        PersistentDataContainer pdc = event.getEntity().getItemStack().getItemMeta().getPersistentDataContainer().get(enchantmentListKey, PersistentDataType.TAG_CONTAINER);
        if (pdc == null) return;
        event.getEntity().setItemStack(getBook(pdc, event.getEntity().getItemStack().getItemMeta().displayName()));
    }
}
