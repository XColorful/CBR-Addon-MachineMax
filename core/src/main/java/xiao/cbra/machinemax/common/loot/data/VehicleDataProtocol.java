package xiao.cbra.machinemax.common.loot.data;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.cbra.machinemax.CbraMachineMax;

import java.util.HashSet;
import java.util.Set;

public class VehicleDataProtocol {

    // 载具ResourceLocation
    public final @NotNull String vehicleLocation;


    @ApiStatus.Internal
    public VehicleDataProtocol(@NotNull String vehicleLocation) {
        this.vehicleLocation = vehicleLocation;
    }

    public static @Nullable VehicleDataProtocol getConfigFromProtocol(String protocol, @NotNull JsonObject jsonTag) {
        if (protocol == null || protocol.isEmpty()) {
            return null;
        }
        String[] parts = protocol.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        String namespace = parts[0];
        String version = parts[1];
        if (namespace.equals(CbraMachineMax.MOD_ID)) {
            switch (version) {
                case "0.5.6" -> {
                    return VehicleDataProtocol056.fromTag(jsonTag);
                }
                default -> {
                    if (!unknownVersion.contains(version)) {
                        CbraMachineMax.LOGGER.info("VehicleDataProtocol: unknown version {}", version);
                        unknownVersion.add(version);
                    }
                    return VehicleDataProtocol056.fromTag(jsonTag);
                }
            }
        }
        return null;
    }

    private static final Set<String> unknownVersion = new HashSet<>();
}
