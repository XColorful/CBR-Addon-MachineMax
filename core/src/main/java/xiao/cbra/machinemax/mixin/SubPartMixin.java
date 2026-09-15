package xiao.cbra.machinemax.mixin;

import io.github.sweetzonzi.machine_max.common.entity.MMPartEntity;
import io.github.sweetzonzi.machine_max.common.vehicle.SubPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiao.battleroyale.BattleRoyale;

import java.util.UUID;

/**
 * 载具部件实体在区块卸载时会随区块被标记移除，{@code SubPart#refreshPartEntity} 会另行创建新实体，
 * 而 CBR 写入的 gameId 保存在旧实体上，因此新实体会丢失 gameId。
 * <p>
 * {@code refreshPartEntity} 是 MachineMax 中创建 {@link MMPartEntity} 的唯一位置，
 * 故在此把旧实体的 gameId 转移到新实体。
 */
@Mixin(SubPart.class)
public abstract class SubPartMixin {

    @Shadow public @Nullable MMPartEntity entity;

    @Unique private @Nullable UUID cbramachinemax$gameId;

    @Inject(method = "refreshPartEntity", at = @At("HEAD"))
    private void cbramachinemax$captureGameId(CallbackInfo ci) {
        MMPartEntity previous = this.entity;
        this.cbramachinemax$gameId = previous == null ? null : BattleRoyale.getGameManager().getGameIdReadApi().getGameId(previous);
    }

    @Inject(method = "refreshPartEntity", at = @At("TAIL"))
    private void cbramachinemax$restoreGameId(CallbackInfo ci) {
        UUID gameId = this.cbramachinemax$gameId;
        MMPartEntity created = this.entity;
        if (gameId == null || created == null) {
            return;
        }
        BattleRoyale.getGameManager().getGameIdWriteApi().addGameId(created, gameId);
    }
}
