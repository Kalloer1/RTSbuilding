package com.rtsbuilding.rtsbuilding.compat.ftb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.server.RtsStorageManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class RtsFtbCompatImpl {
    private final Method teamsApiMethod;
    private final Method getTeamManagerMethod;
    private final Method getTeamForPlayerMethod;
    private final Object serverQuestFileInstance;
    private final Method getOrCreateTeamDataMethod;
    private final Method collectTasksMethod;
    private final Field submitTasksField;
    private final Class<?> itemTaskClass;
    private final Method itemTaskConsumesResourcesMethod;
    private final Method itemTaskOnlyFromCraftingMethod;
    private final Method itemTaskTestMethod;
    private final Method itemTaskGetMaxProgressMethod;
    private final Map<Class<?>, Method> teamDataSetProgressMethodCache = new ConcurrentHashMap<>();

    RtsFtbCompatImpl() throws ReflectiveOperationException {
        Class<?> ftbTeamsApiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
        Class<?> serverQuestFileClass = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
        this.itemTaskClass = Class.forName("dev.ftb.mods.ftbquests.quest.task.ItemTask");

        this.teamsApiMethod = ftbTeamsApiClass.getMethod("api");
        this.getTeamManagerMethod = this.teamsApiMethod.getReturnType().getMethod("getManager");
        this.getTeamForPlayerMethod = resolveTeamLookupMethod(this.getTeamManagerMethod.getReturnType());

        Field serverQuestFileInstanceField = serverQuestFileClass.getField("INSTANCE");
        this.serverQuestFileInstance = serverQuestFileInstanceField.get(null);
        this.getOrCreateTeamDataMethod = findMethodByNameAndArity(serverQuestFileClass, "getOrCreateTeamData", 1);
        this.collectTasksMethod = findOptionalMethod(serverQuestFileClass, "collect", Class.class);
        this.submitTasksField = findOptionalField(serverQuestFileClass, "submitTasks");

        this.itemTaskConsumesResourcesMethod = this.itemTaskClass.getMethod("consumesResources");
        this.itemTaskOnlyFromCraftingMethod = this.itemTaskClass.getMethod("onlyFromCrafting");
        this.itemTaskTestMethod = this.itemTaskClass.getMethod("test", ItemStack.class);
        this.itemTaskGetMaxProgressMethod = this.itemTaskClass.getMethod("getMaxProgress");
    }

    void detectNow(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            Object team = resolveTeam(player.getUUID());
            if (team == null) {
                return;
            }

            Object teamData = this.getOrCreateTeamDataMethod.invoke(this.serverQuestFileInstance, team);
            if (teamData == null) {
                return;
            }

            Collection<?> submitTasks = readSubmitTasks();
            if (submitTasks == null || submitTasks.isEmpty()) {
                return;
            }

            Method setProgressMethod = resolveSetProgressMethod(teamData.getClass());
            for (Object task : submitTasks) {
                if (task == null || !this.itemTaskClass.isInstance(task)) {
                    continue;
                }

                boolean consumesResources = asBoolean(this.itemTaskConsumesResourcesMethod.invoke(task));
                if (consumesResources) {
                    continue;
                }
                boolean onlyFromCrafting = asBoolean(this.itemTaskOnlyFromCraftingMethod.invoke(task));
                if (onlyFromCrafting) {
                    continue;
                }

                long total = countInPlayerInventory(task, player)
                        + RtsStorageManager.countLinkedItemsMatching(player, stack -> testItemTask(task, stack));
                long maxProgress = asLong(this.itemTaskGetMaxProgressMethod.invoke(task));
                long clamped = Math.max(0L, Math.min(total, maxProgress));
                setProgressMethod.invoke(teamData, task, clamped);
            }
        } catch (Throwable throwable) {
            RtsbuildingMod.LOGGER.warn("FTB quest detect run failed for player {}", player.getScoreboardName(), throwable);
        }
    }

    private Object resolveTeam(UUID playerUuid) throws ReflectiveOperationException {
        Object api = this.teamsApiMethod.invoke(null);
        if (api == null) {
            return null;
        }
        Object manager = this.getTeamManagerMethod.invoke(api);
        if (manager == null) {
            return null;
        }
        return this.getTeamForPlayerMethod.invoke(manager, playerUuid);
    }

    private Collection<?> readSubmitTasks() throws ReflectiveOperationException {
        if (this.collectTasksMethod != null) {
            Object collected = this.collectTasksMethod.invoke(this.serverQuestFileInstance, this.itemTaskClass);
            if (collected instanceof Collection<?> collection) {
                return collection;
            }
        }
        if (this.submitTasksField == null) {
            return java.util.List.of();
        }
        Object raw = this.submitTasksField.get(this.serverQuestFileInstance);
        if (raw instanceof Collection<?> collection) {
            return collection;
        }
        return null;
    }

    private Method resolveSetProgressMethod(Class<?> teamDataClass) {
        return this.teamDataSetProgressMethodCache.computeIfAbsent(teamDataClass, cls -> {
            for (Method method : cls.getMethods()) {
                if (!"setProgress".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (!params[0].isAssignableFrom(this.itemTaskClass)) {
                    continue;
                }
                if (params[1] != long.class && params[1] != Long.class) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            throw new IllegalStateException("Missing TeamData.setProgress(task,long) method");
        });
    }

    private long countInPlayerInventory(Object itemTask, ServerPlayer player) {
        long total = 0L;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            if (testItemTask(itemTask, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean testItemTask(Object itemTask, ItemStack stack) {
        try {
            return asBoolean(this.itemTaskTestMethod.invoke(itemTask, stack));
        } catch (ReflectiveOperationException reflectiveOperationException) {
            return false;
        }
    }

    private static Method findMethodByNameAndArity(Class<?> type, String name, int arity) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (!name.equals(method.getName()) || method.getParameterCount() != arity) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Missing method: " + type.getName() + "#" + name + "/" + arity);
    }

    private static Method findOptionalMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Field findFieldByName(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalStateException("Missing field: " + type.getName() + "#" + name);
    }

    private static Field findOptionalField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method resolveTeamLookupMethod(Class<?> managerClass) throws NoSuchMethodException {
        for (String name : new String[] { "getTeamForPlayerID", "getTeamForPlayer" }) {
            try {
                return managerClass.getMethod(name, UUID.class);
            } catch (NoSuchMethodException ignored) {
                // Try next candidate.
            }
        }
        throw new NoSuchMethodException("Missing team lookup method on " + managerClass.getName());
    }

    private static boolean asBoolean(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
