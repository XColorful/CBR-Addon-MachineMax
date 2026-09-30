package xiao.cbra.machinemax.compat.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import xiao.battleroyale.api.common.McSide;
import xiao.cbra.machinemax.CbraMachineMax;

@Mod(CbraMachineMax.MOD_ID)
public class CbraMachineMaxNeoforge {

    public CbraMachineMaxNeoforge() {
        Dist dist = FMLLoader.getDist();
        McSide mcSide = dist.isClient() ? McSide.CLIENT : McSide.DEDICATED_SERVER;

        CbraMachineMax.init(mcSide);
    }
}
