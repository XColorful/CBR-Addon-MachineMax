package xiao.cbra.machinemax;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import xiao.battleroyale.BattleRoyale;
import xiao.battleroyale.api.common.McSide;
import xiao.cbra.machinemax.event.custom.CustomEventHandler;

public class CbraMachineMax {
    public static final String MOD_ID = "cbramachinemax";
    public static final Logger LOGGER = LogUtils.getLogger();

    protected static boolean initialized;

    public static void init(McSide mcSide) {
        if (initialized) return;

        CustomEventHandler.registerAll(BattleRoyale.getEventRegister());

        initialized = true;
    }
}
