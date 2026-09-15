package xiao.cbra.machinemax.compat.neoforge;

import net.neoforged.fml.common.Mod;
import xiao.battleroyale.BattleRoyale;
import xiao.battleroyale.api.common.McSide;
import xiao.cbra.machinemax.CbraMachineMax;

@Mod(CbraMachineMax.MOD_ID)
public class CbraMachineMaxNeoforge {

    public CbraMachineMaxNeoforge() {
        McSide mcSide = BattleRoyale.getMcSide();

        CbraMachineMax.init(mcSide);
    }
}
