package me.flukky.levelmobs.listeners;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.flukky.levelmobs.LevelMobs;

public class Mobslistener implements Listener {
    private LevelMobs plugin;
    private final Random random = new Random();
    private FileConfiguration dropsConfig;

    public Mobslistener(LevelMobs plugin) {
        this.plugin = plugin;
        this.dropsConfig = plugin.getDropsConfig(); // โหลด drops.yml
    }

    @EventHandler
    public void onMonsterDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity monster = (LivingEntity) event.getEntity();

            // ตรวจสอบว่าเป็นมอนสเตอร์ที่มีระดับ (level) โดยมีคำว่า "Level" อยู่ในชื่อ
            if (monster.getCustomName() != null && monster.getCustomName().contains("Lvl")) {
                double newHealth = monster.getHealth() - event.getFinalDamage(); // คำนวณค่า HP หลังจากได้รับดาเมจ

                // ปรับ HP ให้ไม่ต่ำกว่า 0
                if (newHealth < 0) {
                    newHealth = 0;
                }

                // แยกเฉพาะส่วนที่เป็นชื่อและระดับ โดยดึงเฉพาะส่วนก่อน "|"
                String nameWithoutHp = monster.getCustomName().split("\\|")[0].trim(); // ดึงเฉพาะชื่อและระดับ

                // อัปเดตชื่อมอนสเตอร์ด้วย HP ที่เหลืออยู่ โดยเพิ่มสีแดงให้เฉพาะส่วน HP
                String updatedName = String.format("%s | %s❤: %.0f",
                        nameWithoutHp, // ชื่อและระดับเดิม
                        ChatColor.RED, // สีแดงสำหรับ HP
                        newHealth);

                monster.setCustomName(updatedName);
                monster.setCustomNameVisible(true); // แสดงชื่อให้เห็นบนหัว
            }
        }
    }

    @EventHandler
    public void onMonsterDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity monster = (LivingEntity) event.getEntity();

            // ตรวจสอบว่ามอนสเตอร์มีระดับหรือไม่
            if (monster.hasMetadata("level")) {
                int level = monster.getMetadata("level").get(0).asInt();
                dropItemsBasedOnLevel(monster, level);
            }
        }
    }

    public void dropItemsBasedOnLevel(LivingEntity monster, int level) {
        String dropKey = "default"; // กำหนดค่าเริ่มต้น

        // กำหนด dropKey ตามระดับ
        if (level >= 5 && level < 15) {
            dropKey = "low"; // ถ้าระดับอยู่ในช่วง 5 ถึง 14
        } else if (level >= 15 && level < 25) {
            dropKey = "medium"; // ถ้าระดับอยู่ในช่วง 15 ถึง 24
        } else if (level >= 25) {
            dropKey = "high"; // ถ้าระดับ 25 ขึ้นไป
        }
    
        // โหลดไอเท็มจาก drops.yml
        ConfigurationSection itemsSection = dropsConfig.getConfigurationSection("drops." + dropKey + ".items");
    
        if (itemsSection == null) {
            // จัดการเมื่อไม่พบ section
            return; // หรือทำการ log ข้อผิดพลาด
        }
    
        // ใช้ getKeys เพื่อให้ได้คีย์ทั้งหมดใน itemsSection
        for (String itemKey : itemsSection.getKeys(false)) {
            ConfigurationSection itemData = itemsSection.getConfigurationSection(itemKey);
    
            if (itemData == null) {
                continue; // ข้ามไปหากไม่พบข้อมูล
            }
    
            int chanceDrop = itemData.getInt("chance_drop");
            chanceDrop = Math.max(0, Math.min(100, chanceDrop)); // ตรวจสอบให้แน่ใจว่าอยู่ในช่วง 0-100
    
            // คำนวณการดรอป
            if (Math.random() * 100 < chanceDrop) {
                ItemStack item;
                int maxDrop = itemData.getInt("max_drop");
    
                if (itemData.contains("custom_model_data")) {
                    // สำหรับไอเท็มพิเศษ
                    String name = itemData.getString("name");
                    int customModelData = itemData.getInt("custom_model_data");
                    List<String> lore = new ArrayList<>();
                    
                    item = new ItemStack(Material.valueOf(itemKey.toUpperCase()), 1);
                    ItemMeta meta = item.getItemMeta();
    
                    if (meta != null) {
                        if (name.startsWith("Gem of Power +")) {
                            String gemName = ChatColor.DARK_PURPLE + "✦ " + ChatColor.GOLD + name + ChatColor.DARK_PURPLE + " ✦";
                            meta.setDisplayName(gemName);
                            lore.add(ChatColor.GRAY + "─────────────"); // เส้นคั่นสวยงาม
                            lore.add(ChatColor.YELLOW + "ใช้เพื่อเสริมพลังอาวุธและเกราะของคุณ");
                            lore.add(ChatColor.YELLOW + "ช่วยเพิ่มโอกาสในการอัปเกรด แต่ระวังความล้มเหลว!");
                            lore.add("");
                            lore.add(ChatColor.LIGHT_PURPLE + "พลังที่ถูกซ่อนอยู่จะปลดปล่อย");
                            lore.add(ChatColor.LIGHT_PURPLE + "เมื่อใช้อัญมณีนี้ในการอัปเกรด");
                            lore.add(ChatColor.LIGHT_PURPLE + "ใชัอัปเกรดอุปกรณ์เพื่อให้แข็งแกร่งที่สุด");
                            lore.add(ChatColor.GRAY + "─────────────");
                        }
                        meta.setCustomModelData(customModelData);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                } else {
                    // สำหรับไอเท็มปกติ
                    item = new ItemStack(Material.valueOf(itemKey.toUpperCase()), 1);
                }
    
                // จำนวนที่ดรอปสุ่ม
                int dropAmount = (int) (Math.random() * maxDrop) + 1; 
                for (int i = 0; i < dropAmount; i++) {
                    monster.getWorld().dropItem(monster.getLocation(), item);
                }
            }
        }
    }       

    @EventHandler
    public void onPlayerKillMonster(EntityDeathEvent event) {
        // ตรวจสอบว่าผู้เล่นคือผู้ที่ฆ่ามอนสเตอร์หรือไม่
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = event.getEntity().getKiller();
            int currentLevel = plugin.getPlayerLevels().getOrDefault(player, 1); // ถ้ายังไม่มีระดับให้ตั้งเป็น 1

            // เพิ่มระดับขึ้นตามเงื่อนไขที่กำหนด
            int newLevel = currentLevel + 1; // หรือเพิ่มตามเงื่อนไขของคุณ
            plugin.updatePlayerLevel(player, newLevel);
        }
    }

    @EventHandler
    public void onMonsterExplode(EntityExplodeEvent event) {
        // ตรวจสอบว่ามอนสเตอร์เป็น LivingEntity
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity monster = (LivingEntity) event.getEntity();

            // ตรวจสอบว่ามอนสเตอร์มีระดับหรือไม่
            if (monster.getCustomName() != null && monster.getCustomName().contains("Lvl")) {
                double damage = monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue();
                Entity target = event.getEntity();

                // ทำให้ระเบิดสร้างความเสียหาย
                event.getLocation().getWorld().createExplosion(event.getLocation(), (float) 5);

                // ส่งข้อความถึงผู้เล่นทุกคนเกี่ยวกับความเสียหายที่เกิดขึ้น
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.RED + monster.getCustomName() + " ทำความเสียหาย: " + damage + " ไปยัง " + target.getType().name());
                }
            }
        }
    }

    @EventHandler
    public void onMonsterAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity monster = (LivingEntity) event.getDamager();

            // ตรวจสอบว่ามอนสเตอร์มีระดับหรือไม่
            if (monster.getCustomName() != null && monster.getCustomName().contains("Lvl")) {
                Entity target = event.getEntity();
                
                // ดึงค่าดาเมจที่ตั้งไว้แล้วของมอนสเตอร์
                double damage = monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue();
                
                // ตั้งค่าความเสียหายให้กับเป้าหมายโดยตรง
                event.setDamage(damage);

                // ส่งข้อความถึงผู้เล่นทุกคนเกี่ยวกับความเสียหายที่เกิดขึ้น
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.RED + monster.getCustomName() + " ทำความเสียหาย: " + damage + " ไปยัง " + target.getType().name());
                }
            }
        }
    }

}
