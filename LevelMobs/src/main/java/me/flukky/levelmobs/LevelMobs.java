package me.flukky.levelmobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import me.flukky.levelmobs.commands.spawnmobs;
import me.flukky.levelmobs.listeners.Mobslistener;
import me.flukky.levelmobs.managers.TypeMobs;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class LevelMobs extends JavaPlugin implements Listener {
    private int lowLevelChance;
    private int midLevelChance;
    private int highLevelChance;
    private int minLevel;
    private int maxLevel;

    private double healthMultiplierPerLevel;
    private double damageMultiplierPerLevel;
    private double baseSpeed;
    private double speedMultiplierPerLevel;
    private double baseXp;
    private double xpMultiplierPerLevel;
    private double spawnChance;

    private String lowLevelColor;
    private String midLevelColor;
    private String highLevelColor;
    private String extremeLevelColor;

    private TypeMobs typeMobs;
    private FileConfiguration dropsConfig;

    private HashMap<Player, Integer> playerLevels = new HashMap<>(); // ใช้ในการเก็บระดับของผู้เล่น

    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

    @Override
    public void onEnable() {
        saveDefaultConfig(); // บันทึกค่า config เริ่มต้นถ้ายังไม่มี
        loadConfigValues(); // โหลดค่าจาก config
        reloadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new Mobslistener(this), this);

        typeMobs = new TypeMobs(this);

        console.sendMessage("LevelMobs has been enabled!");

        spawnmobs monsterCommands = new spawnmobs(this);
        this.getCommand("levelmobs").setExecutor(monsterCommands);
    }

    public void loadConfigValues() {
        FileConfiguration config = getConfig();
        lowLevelChance = config.getInt("level-chances.low-level-chance");
        midLevelChance = config.getInt("level-chances.mid-level-chance");
        highLevelChance = config.getInt("level-chances.high-level-chance");
        minLevel = config.getInt("min-level");
        maxLevel = config.getInt("max-level");
        spawnChance = config.getDouble("spawn-chance");

        healthMultiplierPerLevel = config.getDouble("health-multiplier-per-level");
        damageMultiplierPerLevel = config.getDouble("damage-multiplier-per-level");
        baseSpeed = config.getDouble("base-speed");
        speedMultiplierPerLevel = config.getDouble("speed-multiplier-per-level");
        baseXp = config.getDouble("base-xp");
        xpMultiplierPerLevel = config.getDouble("xp-multiplier-per-level");

        lowLevelColor = config.getString("level-colors.low-level-color").toUpperCase();
        midLevelColor = config.getString("level-colors.mid-level-color").toUpperCase();
        highLevelColor = config.getString("level-colors.high-level-color").toUpperCase();
        extremeLevelColor = config.getString("level-colors.extreme-level-color").toUpperCase();

        console.sendMessage(
                "Config values loaded: lowLevelChance=" + lowLevelChance + ", midLevelChance=" + midLevelChance +
                        ", highLevelChance=" + highLevelChance + ", minLevel=" + minLevel + ", maxLevel=" + maxLevel);
    }

    public FileConfiguration getDropsConfig() {
        dropsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "drops.yml"));
        return dropsConfig;
    }

    // เมธอดสำหรับอัปเดตระดับของผู้เล่น (อาจมีในส่วนอื่นของโค้ด)
    public void updatePlayerLevel(Player player, int level) {
        playerLevels.put(player, level);
    }

    public HashMap<Player, Integer> getPlayerLevels() {
        return playerLevels;
    }

    public double getMultiplierDamage() {
        return damageMultiplierPerLevel;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // ตรวจสอบว่า Entity เป็น LivingEntity
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity monster = (LivingEntity) event.getEntity();

            // ตรวจสอบว่ามอนสเตอร์เป็นประเภทที่สามารถสุ่มระดับได้
            if (typeMobs.isMonsterType(monster.getType())) {
                Random random = new Random();

                // ปรับโอกาสที่มอนสเตอร์จะมีระดับ (0.2 = 20% โอกาส)
                boolean hasLevel = random.nextDouble() < spawnChance; // เปลี่ยนค่า 0.2 เพื่อปรับโอกาสที่ต้องการ

                if (hasLevel) {
                    int level;
                    int chance = random.nextInt(100); // สุ่มค่า 0-99

                    // สุ่มระดับตามโอกาสที่กำหนดใน config
                    if (chance < lowLevelChance) {
                        level = random.nextInt(20) + 1; // ระดับ 1-20
                    } else if (chance < lowLevelChance + midLevelChance) {
                        level = random.nextInt(15) + 21; // ระดับ 21-35
                    } else {
                        level = random.nextInt(15) + 36; // ระดับ 36-50
                    }

                    /* // ปรับให้ระดับสูงมีโอกาสน้อยลงถ้าจำเป็น
                    if (level > 35 && random.nextDouble() > 0.05) { // 95% โอกาสที่จะไม่สุ่มระดับสูง
                        level = random.nextInt(35) + 1; // เปลี่ยนไปให้ต่ำกว่าระดับสูง
                    } */

                    console.sendMessage("Monster spawned: " + monster.getType().name() + " - Level: " + level);

                    // ตั้งค่า metadata
                    monster.setMetadata("level", new FixedMetadataValue(this, level)); // ตั้งค่า level ใน metadata

                    // คำนวณสุขภาพและความเสียหายตามระดับ
                    double health = getAttributeValue(monster, org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)
                            + (level - 1) * healthMultiplierPerLevel;
                    double damage = getAttributeValue(monster, org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) + (level - 1) * damageMultiplierPerLevel;

                    double speed = baseSpeed + (level - 1) * speedMultiplierPerLevel;
                    double xp = baseXp + (level - 1) * xpMultiplierPerLevel;

                    // กำหนดสีตามระดับ
                    String levelColor = determineLevelColor(level);

                    // ตั้งชื่อและสุขภาพ
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
                    monster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
                    monster.setHealth(health);
                    monster.setMaxHealth(health);

                    // ตั้งค่าอาวุธและชุดเกราะตามระดับ
                    equipEntityWithArmorAndWeapon(monster, level);

                    console.sendMessage("Monster updated: " + monster.getCustomName() + " | Health: " + health
                            + " | Damage: " + damage);
                } else {
                    console.sendMessage("Monster spawned: " + monster.getType().name() + " - No Level assigned.");
                }
            }
        }
    }

    private double getAttributeValue(LivingEntity entity, org.bukkit.attribute.Attribute attribute) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            return attributeInstance.getBaseValue();
        } else {
            getLogger().warning("Attribute " + attribute + " is not present for " + entity.getType());
            return 0; // คืนค่า 0 หรือค่าที่เหมาะสมตามที่คุณต้องการ
        }
    }

    public String determineLevelColor(int level) {
        if (level <= 10) {
            return lowLevelColor;
        } else if (level <= 20) {
            return midLevelColor;
        } else if (level <= 30) {
            return highLevelColor;
        } else {
            return extremeLevelColor;
        }
    }

    private void equipEntityWithArmorAndWeapon(LivingEntity entity, int level) {
        if (entity instanceof Monster) {
            // ตั้งค่าชุดเกราะ
            if (level >= 5) {
                entity.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            }
            if (level >= 10) {
                entity.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                entity.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            }
            if (level >= 15) {
                entity.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                entity.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            }
            if (level >= 20) {
                entity.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                entity.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
            }

            // ตั้งค่าอาวุธ
            if (level >= 5) {
                entity.getEquipment().setItemInHand(new ItemStack(Material.GOLDEN_AXE));
            }
        }
    }

    /*
     * @EventHandler
     * public void onCreatureSpawn(CreatureSpawnEvent event) {
     * if (event.getEntity() instanceof LivingEntity) {
     * LivingEntity monster = (LivingEntity) event.getEntity();
     * 
     * // ตรวจสอบว่ามอนสเตอร์เป็นประเภทที่สามารถสุ่มระดับได้
     * if (isMonsterType(monster.getType())) {
     * Random random = new Random();
     * int maxLevel = 0;
     * 
     * // ตรวจสอบระดับของผู้เล่นในโลกนี้
     * for (Player player : monster.getWorld().getPlayers()) {
     * if (playerLevels.containsKey(player)) {
     * int playerLevel = playerLevels.get(player);
     * if (playerLevel > maxLevel) {
     * maxLevel = playerLevel; // หาระดับที่สูงที่สุดของผู้เล่นในโลก
     * }
     * }
     * }
     * 
     * // กำหนดระดับของมอนสเตอร์ตามระดับของผู้เล่น
     * int level = Math.max(1, maxLevel); // มอนสเตอร์มีระดับต่ำสุด 1
     * double health = getAttributeValue(monster,
     * org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) + (level - 1) *
     * healthMultiplierPerLevel;
     * double damage = getAttributeValue(monster,
     * org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE) + (level - 1) *
     * damageMultiplierPerLevel;
     * 
     * // ตั้งชื่อมอนสเตอร์
     * String entityName = String.format("Level %d %s", level,
     * monster.getType().name());
     * monster.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).
     * setBaseValue(health);
     * monster.setCustomName(entityName);
     * monster.setCustomNameVisible(true);
     * monster.setHealth(health);
     * monster.setMaxHealth(health);
     * 
     * // แสดงข้อความในคอนโซล
     * console.sendMessage("Monster spawned: " + monster.getCustomName() +
     * " | Health: " + health + " | Damage: " + damage);
     * }
     * }
     * }
     */

}
