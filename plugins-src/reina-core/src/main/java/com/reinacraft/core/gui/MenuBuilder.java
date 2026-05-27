package com.reinacraft.core.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MenuBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Component title;
    private int size = 27;
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final Map<Integer, Menu.ClickHandler> handlers = new HashMap<>();
    private boolean cancelClicks = true;
    private boolean glassBorder = true;
    private Material borderPrimary = Material.BLACK_STAINED_GLASS_PANE;
    private Material borderAccent = Material.GRAY_STAINED_GLASS_PANE;

    public static MenuBuilder of(String mmTitle) {
        MenuBuilder b = new MenuBuilder();
        b.title = MM.deserialize(mmTitle);
        return b;
    }

    public static MenuBuilder of(Component title) {
        MenuBuilder b = new MenuBuilder();
        b.title = title;
        return b;
    }

    public MenuBuilder rows(int rows) {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("rows 1..6");
        this.size = rows * 9;
        return this;
    }

    public MenuBuilder allowClicks() { this.cancelClicks = false; return this; }
    public MenuBuilder noBorder() { this.glassBorder = false; return this; }

    public MenuBuilder borderColors(Material primary, Material accent) {
        this.borderPrimary = primary;
        this.borderAccent = accent;
        return this;
    }

    public MenuBuilder item(int slot, ItemStack item) {
        items.put(slot, item);
        return this;
    }

    public MenuBuilder item(int slot, ItemStack item, Menu.ClickHandler handler) {
        items.put(slot, item);
        handlers.put(slot, handler);
        return this;
    }

    public MenuBuilder item(int slot, Material mat, String mmName, List<String> mmLore, Menu.ClickHandler handler) {
        items.put(slot, namedItem(mat, mmName, mmLore));
        if (handler != null) handlers.put(slot, handler);
        return this;
    }

    public MenuBuilder item(int slot, Material mat, String mmName, Menu.ClickHandler handler) {
        return item(slot, mat, mmName, List.of(), handler);
    }

    public Menu build() {
        if (glassBorder) applyGlassBorder();
        return new Menu(title, size, items, handlers, cancelClicks);
    }

    private void applyGlassBorder() {
        int rows = size / 9;
        for (int col = 0; col < 9; col++) {
            int top = col;
            int bottom = (rows - 1) * 9 + col;
            putIfAbsent(top, borderItem(col));
            putIfAbsent(bottom, borderItem(col + rows));
        }
        for (int r = 1; r < rows - 1; r++) {
            putIfAbsent(r * 9, borderItem(r));
            putIfAbsent(r * 9 + 8, borderItem(r + 1));
        }
    }

    private void putIfAbsent(int slot, ItemStack stack) {
        if (!items.containsKey(slot)) items.put(slot, stack);
    }

    private ItemStack borderItem(int salt) {
        Material mat = ((salt & 1) == 0) ? borderPrimary : borderAccent;
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(" "));
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack namedItem(Material mat, String mmName, List<String> mmLore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(MM.deserialize(mmName).decoration(TextDecoration.ITALIC, false));
        if (!mmLore.isEmpty()) {
            List<Component> lore = new ArrayList<>(mmLore.size());
            for (String line : mmLore) {
                lore.add(MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack namedItem(Material mat, String mmName, String... mmLore) {
        return namedItem(mat, mmName, Arrays.asList(mmLore));
    }
}
