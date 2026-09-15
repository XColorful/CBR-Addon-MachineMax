package xiao.cbra.machinemax.common.loot.data;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import xiao.battleroyale.util.JsonUtils;
import xiao.cbra.machinemax.CbraMachineMax;

// cbramachinemax:0.5.6
public class VehicleDataProtocol056 {

    // 载具ResourceLocation
    public static final String VEHICLE_LOCATION = "vehicleLocation";

    protected static VehicleDataProtocol fromTag(@NotNull JsonObject jsonTag) {
        try {
            String vehicleLocation = JsonUtils.getJsonString(jsonTag, VEHICLE_LOCATION, "");

            return new VehicleDataProtocol(vehicleLocation);
        } catch (Exception e) {
            CbraMachineMax.LOGGER.warn("VehicleDataProtocol056: Failed to parse by cbramachinemax:0.5.6 protocol from jsonTag: {}", jsonTag, e);
            return null;
        }
    }
}
