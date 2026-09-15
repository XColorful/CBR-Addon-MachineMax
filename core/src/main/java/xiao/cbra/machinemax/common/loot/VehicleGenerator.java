package xiao.cbra.machinemax.common.loot;

import io.github.sweetzonzi.machine_max.common.vehicle.ObjectManager;
import io.github.sweetzonzi.machine_max.common.vehicle.Part;
import io.github.sweetzonzi.machine_max.common.vehicle.SubPart;
import io.github.sweetzonzi.machine_max.common.vehicle.VehicleCore;
import io.github.sweetzonzi.machine_max.common.vehicle.data.VehicleData;
import io.github.sweetzonzi.machine_max.external.MMDynamicRes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xiao.battleroyale.BattleRoyale;
import xiao.battleroyale.api.game.IGameIdWriteApi;
import xiao.battleroyale.api.loot.data.ILootData;
import xiao.battleroyale.common.loot.LootGenerator;
import xiao.cbra.machinemax.CbraMachineMax;
import xiao.cbra.machinemax.common.loot.data.VehicleDataProtocol;

import java.util.List;
import java.util.UUID;

public class VehicleGenerator {

    /**
     * <ul>
     *     刷新载具实体
     *     <li>在方块处刷新</li>
     *     <li>每个载具部件(实体)都写入 {@link LootGenerator.LootContext#gameId}</li>
     *     <li>刷出来的就能直接开</li>
     *     <li>不生成lootData数据</li>
     * </ul>
     */
    public static void generateVehicle(LootGenerator.LootContext lootContext, BlockEntity targetBlockEntity, List<ILootData> lootData,
                                       VehicleDataProtocol protocol) {
        @Nullable var vehicleLocation = ResourceLocation.tryParse(protocol.vehicleLocation);
        if (vehicleLocation == null) {
            CbraMachineMax.LOGGER.debug("VehicleGenerator: Invalid vehicleLocation {}", protocol.vehicleLocation);
            return;
        }
        // 模板(templates/)是 MachineMax 中唯一承载载具结构数据的注册表，其注册ID即载具标识
        VehicleData vehicleData = MMDynamicRes.TEMPLATES.get(vehicleLocation);
        if (vehicleData == null) {
            CbraMachineMax.LOGGER.debug("VehicleGenerator: Unknown vehicle template {}", vehicleLocation);
            return;
        }

        BlockPos spawnOrigin = targetBlockEntity.getBlockPos();
        // 载具包围盒底面贴合方块顶面，与蓝图/装配体物品的放置方式一致
        Vec3 spawnPos = new Vec3(spawnOrigin.getX() + 0.5,
                spawnOrigin.getY() - vehicleData.min.y,
                spawnOrigin.getZ() + 0.5);

        IGameIdWriteApi gameIdWriteApi = BattleRoyale.getGameManager().getGameIdWriteApi();
        try {
            VehicleCore vehicle = new VehicleCore(lootContext.serverLevel,
                    vehicleData.withNewUUID(UUID.randomUUID()),
                    true); // true是刷出来就能开
            vehicle.setPos(spawnPos);
            ObjectManager.addVehicle(vehicle);

            for (Part part : vehicle.partMap.values()) {
                for (SubPart subPart : part.subParts.values()) {
                    if (subPart.entity == null) {
                        subPart.refreshPartEntity();
                    }
                    gameIdWriteApi.addGameId(subPart.entity, lootContext.gameId);
                }
            }
        } catch (Exception e) {
            CbraMachineMax.LOGGER.warn("VehicleGenerator: Failed to generate vehicle {} at {}", vehicleLocation, spawnOrigin, e);
            return;
        }

        CbraMachineMax.LOGGER.debug("VehicleGenerator::generate {} {}", vehicleLocation, spawnOrigin);
    }
}
