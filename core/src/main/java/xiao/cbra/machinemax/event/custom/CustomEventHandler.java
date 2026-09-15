package xiao.cbra.machinemax.event.custom;

import xiao.battleroyale.api.event.CustomEventType;
import xiao.battleroyale.api.event.ICustomEventRegister;
import xiao.cbra.machinemax.common.loot.VehicleGeneratorManager;

public class CustomEventHandler {

    public static void registerAll(ICustomEventRegister eventRegister) {
        eventRegister.register(VehicleGeneratorManager.get(), CustomEventType.CUSTOM_GENERATE_EVENT);
    }
}
