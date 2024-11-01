package me.flukky.levelmobs.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.entity.EntityType;

import me.flukky.levelmobs.LevelMobs;

public class spawnmobs implements CommandExecutor {
    private final LevelMobs plugin;
    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

    public spawnmobs(LevelMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("levelmobs")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig(); // โหลด config ใหม่
                plugin.loadConfigValues(); // โหลดค่าจาก config
                sender.sendMessage(ChatColor.GREEN + "LevelMobs config reloaded!");
                return true;
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("spawn")) {
                return spawnMonsterCommand(sender, args);
            }

            // ถ้าใช้คำสั่งไม่ถูกต้อง ให้แสดงคำอธิบายคำสั่ง
            sender.sendMessage(ChatColor.RED + "Usage: /levelmobs spawn <monster_name> <level> or /levelmobs reload");
            return false;
        }
        return false;
    }

    private boolean spawnMonsterCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /levelmobs spawn <monster_name> <level>");
            return false;
        }

        String typeMonster = args[1]; // เปลี่ยนเพื่อให้ monster_name อยู่ที่ args[1]
        int level;

        try {
            level = Integer.parseInt(args[2]); // เปลี่ยน index เพื่อให้ level อยู่ที่ args[2]
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Level must be a number.");
            return false;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Location location = player.getLocation();
            
            // สร้าง EntityType จากชื่อที่ได้รับ
            EntityType monsterType;
            try {
                monsterType = EntityType.valueOf(typeMonster.toUpperCase()); // แปลงชื่อให้เป็นตัวพิมพ์ใหญ่
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid monster type.");
                return false;
            }

            // Spawn the monster
            String levelColor = plugin.determineLevelColor(level);
            LivingEntity monster = (LivingEntity) location.getWorld().spawnEntity(location, monsterType);

            // ตั้งค่า metadata
            monster.setMetadata("level", new FixedMetadataValue(plugin, level)); // ตั้งค่า level ใน metadata

            // Set health and damage based on level
            double health = plugin.getConfig().getDouble("health-multiplier-per-level") * (level - 1) + 20; // 20
                                                                                                            // ค่าสุขภาพเริ่มต้น
            double damageMultiplier = plugin.getConfig().getDouble("damage-multiplier-per-level");

            double damage = getAttributeValue(monster, Attribute.GENERIC_ATTACK_DAMAGE) + (level - 1) * damageMultiplier;

            String entityName = String.format("%sLvl %d %s%s | %s❤: %.0f",
                    ChatColor.valueOf(levelColor), // สีของ Level
                    level,
                    ChatColor.WHITE, // สีของชื่อมอนสเตอร์ (สีขาว)
                    monster.getType().name(),
                    ChatColor.RED, // สีของ HP (สีแดง)
                    health);

            monster.setCustomName(entityName);
            monster.setCustomNameVisible(true); // แสดงชื่อให้เห็นบนหัว
            monster.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
            monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
            monster.setHealth(health);
            monster.setMaxHealth(health);

            console.sendMessage(ChatColor.YELLOW + "Calculated Damage: " + damage);

            sender.sendMessage(ChatColor.GREEN + "Spawned " + typeMonster + " at level " + level);
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
        }

        return true;
    }

    private double getAttributeValue(LivingEntity entity, org.bukkit.attribute.Attribute attribute) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            return attributeInstance.getBaseValue();
        } else {
            plugin.getLogger().warning("Attribute " + attribute + " is not present for " + entity.getType());
            return 0; // คืนค่า 0 หรือค่าที่เหมาะสมตามที่คุณต้องการ
        }
    }
}
