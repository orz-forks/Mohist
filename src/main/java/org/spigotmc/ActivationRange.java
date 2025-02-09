package org.spigotmc;

import co.aikar.timings.MinecraftTimings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.effect.EntityWeatherEffect;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;

public class ActivationRange {

    static AxisAlignedBB maxBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB miscBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB animalBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB monsterBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

    /**
     * Initializes an entities type on construction to specify what group this
     * entity is in for activation ranges.
     *
     * @param entity
     * @return group id
     */
    public static byte initializeEntityActivationType(Entity entity) {
        if (entity instanceof EntityMob || entity instanceof EntitySlime)
        {
            return 1; // Monster
        } else if (entity instanceof EntityCreature || entity instanceof EntityAmbientCreature) {
            return 2; // Animal
        } else {
            return 3; // Misc
        }
    }

    /**
     * These entities are excluded from Activation range checks.
     *
     * @param entity
     * @return boolean If it should always tick.
     */
    public static boolean initializeEntityActivationState(Entity entity, SpigotWorldConfig config) {
        if (config == null && DimensionManager.getWorld(0) != null) {
            config = DimensionManager.getWorld(0).spigotConfig;
        } else {
            return true;
        }

        if ((entity.activationType == 3 && config.miscActivationRange == 0)
                || (entity.activationType == 2 && config.animalActivationRange == 0)
                || (entity.activationType == 1 && config.monsterActivationRange == 0)
                || entity instanceof EntityPlayer
                || entity instanceof EntityThrowable
                || entity instanceof MultiPartEntityPart
                || entity instanceof EntityWither
                || entity instanceof EntityFireball
                || entity instanceof EntityWeatherEffect
                || entity instanceof EntityTNTPrimed
                || entity instanceof EntityFallingBlock // Paper - Always tick falling blocks
                || entity instanceof EntityEnderCrystal
                || entity instanceof EntityFireworkRocket) {
            return true;
        }

        return false;
    }

    /**
     * Find what entities are in range of the players in the world and set
     * active if in range.
     *
     * @param world
     */
    public static void activateEntities(World world) {
        MinecraftTimings.entityActivationCheckTimer.startTiming();
        final int miscActivationRange = world.spigotConfig.miscActivationRange;
        final int animalActivationRange = world.spigotConfig.animalActivationRange;
        final int monsterActivationRange = world.spigotConfig.monsterActivationRange;

        int maxRange = Math.max(monsterActivationRange, animalActivationRange);
        maxRange = Math.max(maxRange, miscActivationRange);
        maxRange = Math.min((world.spigotConfig.viewDistance << 4) - 8, maxRange);

        for (EntityPlayer player : world.playerEntities) {

            player.activatedTick = MinecraftServer.currentTick;
            maxBB = player.getEntityBoundingBox().grow(maxRange, 256, maxRange);
            miscBB = player.getEntityBoundingBox().grow(miscActivationRange, 256, miscActivationRange);
            animalBB = player.getEntityBoundingBox().grow(animalActivationRange, 256, animalActivationRange);
            monsterBB = player.getEntityBoundingBox().grow(monsterActivationRange, 256, monsterActivationRange);

            int i = MathHelper.floor(maxBB.minX / 16.0D);
            int j = MathHelper.floor(maxBB.maxX / 16.0D);
            int k = MathHelper.floor(maxBB.minZ / 16.0D);
            int l = MathHelper.floor(maxBB.maxZ / 16.0D);

            for (int i1 = i; i1 <= j; ++i1) {
                for (int j1 = k; j1 <= l; ++j1) {
                    if (world.getWorld().isChunkLoaded(i1, j1)) {
                        activateChunkEntities(world.getChunkFromChunkCoords(i1, j1));
                    }
                }
            }
        }
        MinecraftTimings.entityActivationCheckTimer.stopTiming();
    }

    /**
     * Checks for the activation state of all entities in this chunk.
     *
     * @param chunk
     */
    private static void activateChunkEntities(Chunk chunk) {
        for (ClassInheritanceMultiMap<Entity> slice : chunk.entityLists) {
            for (Entity entity : slice) {
				if (entity == null) continue;
                if (MinecraftServer.currentTick > entity.activatedTick) {
                    if (entity.defaultActivationState) {
                        entity.activatedTick = MinecraftServer.currentTick;
                        continue;
                    }
                    switch (entity.activationType) {
                        case 1:
                            if (monsterBB.intersects(entity.getEntityBoundingBox())) {
                                entity.activatedTick = MinecraftServer.currentTick;
                            }
                            break;
                        case 2:
                            if (animalBB.intersects(entity.getEntityBoundingBox())) {
                                entity.activatedTick = MinecraftServer.currentTick;
                            }
                            break;
                        case 3:
                        default:
                            if (miscBB.intersects(entity.getEntityBoundingBox())) {
                                entity.activatedTick = MinecraftServer.currentTick;
                            }
                    }
                }
            }
        }
    }

    /**
     * If an entity is not in range, do some more checks to see if we should
     * give it a shot.
     *
     * @param entity
     * @return
     */
    public static boolean checkEntityImmunities(Entity entity) {
        // quick checks.
        if (entity.isInWater() || entity.fire > 0) {
            return true;
        }
        if (!(entity instanceof EntityArrow)) {
            if (!entity.onGround || !entity.riddenByEntities.isEmpty() || entity.isRiding()) {
                return true;
            }
        } else if (!((EntityArrow) entity).inGround) {
            return true;
        }
        // special cases.
        if (entity instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) entity;

            if ( /*TODO: Missed mapping? living.attackTicks > 0 || */ living.hurtTime > 0 || living.activePotionsMap.size() > 0) {
                return true;
            }
            if (entity instanceof EntityCreature && ((EntityCreature) entity).getAttackTarget() != null) {
                return true;
            }
            if (entity instanceof EntityVillager && ((EntityVillager) entity).isMating()/* Getter for first boolean */) {
                return true;
            }
            if (entity instanceof EntityAnimal) {
                EntityAnimal animal = (EntityAnimal) entity;
                if (animal.isChild() || animal.isInLove()) {
                    return true;
                }
                if (entity instanceof EntitySheep && ((EntitySheep) entity).getSheared()) {
                    return true;
                }
            }
            if (entity instanceof EntityCreeper && ((EntityCreeper) entity).hasIgnited()) { // isExplosive
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the entity is active for this tick.
     *
     * @param entity
     * @return
     */
    public static boolean checkIfActive(Entity entity) {
        // Never safe to skip fireworks or entities not yet added to chunk
        // PAIL: inChunk
        if (!entity.addedToChunk || entity instanceof EntityFireworkRocket) {
            return true;
        }

        boolean isActive = entity.activatedTick >= MinecraftServer.currentTick || entity.defaultActivationState;

        // Should this entity tick?
        if (!isActive) {
            if ((MinecraftServer.currentTick - entity.activatedTick - 1) % 20 == 0) {
                // Check immunities every 20 ticks.
                if (checkEntityImmunities(entity)) {
                    // Triggered some sort of immunity, give 20 full ticks before we check again.
                    entity.activatedTick = MinecraftServer.currentTick + 20;
                }
                isActive = true;
            }
            // Add a little performance juice to active entities. Skip 1/4 if not immune.
        } else if (!entity.defaultActivationState && entity.ticksExisted % 4 == 0 && !checkEntityImmunities(entity)) {
            isActive = false;
        }
        int x = MathHelper.floor(entity.posX);
        int z = MathHelper.floor(entity.posZ);
        // Make sure not on edge of unloaded chunk
        Chunk chunk = entity.world.getChunkIfLoaded(x >> 4, z >> 4);
        if (isActive && !(chunk != null && chunk.areNeighborsLoaded(1))) {
            isActive = false;
        }
        return isActive;
    }
}
