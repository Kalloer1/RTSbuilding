package com.rtsbuilding.rtsbuilding.server.camera;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;

import java.util.UUID;
import java.util.function.Predicate;

final class RtsCameraEntityHelper {

    private static final String CAMERA_TAG = "rts_camera";

    private RtsCameraEntityHelper() {
    }

    static Entity findCameraEntity(MinecraftServer server, UUID cameraUuid) {
        if (server == null || cameraUuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(cameraUuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    static void discardOwnedCameras(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return;
        }
        UUID ownerUuid = player.getUUID();
        for (ServerLevel level : player.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AreaEffectCloud cloud
                        && cloud.getTags().contains(CAMERA_TAG)
                        && ownerUuid.equals(getOwnerUuid(cloud))) {
                    cloud.discard();
                }
            }
        }
    }

    static AreaEffectCloud createAndSpawnCamera(ServerLevel level, UUID ownerUuid,
            double x, double y, double z, float yaw, float pitch) {
        AreaEffectCloud camera = new AreaEffectCloud(level, x, y, z);
        camera.setNoGravity(true);
        camera.noPhysics = true;
        camera.setDuration(Integer.MAX_VALUE);
        camera.setWaitTime(0);
        camera.setRadius(0.0F);
        camera.setInvisible(true);
        camera.addTag(CAMERA_TAG);
        setOwnerUuid(camera, ownerUuid);
        snapTo(camera, x, y, z, yaw, pitch);
        level.addFreshEntity(camera);
        return camera;
    }

    static void cleanupOrphanCameras(MinecraftServer server, Predicate<UUID> isActiveCamera) {
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AreaEffectCloud cloud
                        && cloud.getTags().contains(CAMERA_TAG)
                        && !isActiveCamera.test(cloud.getUUID())) {
                    cloud.discard();
                }
            }
        }
    }

    static UUID getOwnerUuid(AreaEffectCloud cloud) {
        var tag = cloud.getPersistentData();
        if (tag.hasUUID("rts_owner")) {
            return tag.getUUID("rts_owner");
        }
        return null;
    }

    static void setOwnerUuid(AreaEffectCloud cloud, UUID ownerUuid) {
        cloud.getPersistentData().putUUID("rts_owner", ownerUuid);
    }

    static void snapTo(AreaEffectCloud cloud, double x, double y, double z, float yaw, float pitch) {
        cloud.setPos(x, y, z);
        cloud.setYRot(yaw);
        cloud.setXRot(pitch);
        cloud.setYHeadRot(yaw);
        cloud.setYBodyRot(yaw);
        cloud.xRotO = pitch;
        cloud.yRotO = yaw;
    }
}