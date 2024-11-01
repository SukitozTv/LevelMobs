package me.flukky.levelmobs.managers;

import org.bukkit.entity.EntityType;

import me.flukky.levelmobs.LevelMobs;

public class TypeMobs {
    private LevelMobs plugin;

    public TypeMobs(LevelMobs plugin) {
        this.plugin = plugin;
    }

    // ตรวจสอบว่า EntityType เป็นมอนสเตอร์หรือไม่
    public boolean isMonsterType(EntityType type) {
        switch (type) {
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            //case ENDERMAN:
            case WITCH:
            case GHAST:
            //case SLIME:
            case MAGMA_CUBE:
            case BLAZE:
            case WITHER_SKELETON:
            case STRAY:
            case HUSK:
            case PHANTOM:
            case DROWNED:
            case PILLAGER:
            case EVOKER:
            case VINDICATOR:
            case RAVAGER:
            case ILLUSIONER:
            case ZOMBIE_VILLAGER:
                return true;
            default:
                return false;
        }
    }
}
