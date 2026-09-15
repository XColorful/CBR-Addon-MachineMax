> #1 提示词

# 

```
现在需要完成以下任务：
- 调查MachineMax里载具的标识是什么，需要哪些信息来确定一个载具，如果需要多个信息，则按已有方式补充到xiao.cbra.machinemax.common.loot.data.VehicleDataProtocol和VehicleDataProtocol056
- 在xiao.cbra.machinemax.common.loot.VehicleGenerator#generateVehicle里生成MachineMax载具，注意每个载具part Entity都要用gameIdWriteApi写入gameId
- 生成载具的方式，应该跟玩家使用物品生成载具的效果相同（默认状态)

- 注意当前打开的主窗口是CBR，而修改的代码需要写在CBR-Addon-MachineMax
- CBR-Addon-MachineMax里的MachineMax版本较旧，MachineMax目录下是fork的最新进度，但由于MachineMax的构建环境前置依赖无法公开获取的部分，因此最终修改以旧版本为准，最新fork仅供参考

目前已经跑通VehicleGenerator里的log，只需要继续完成实体生成即可

现在开始研究并完成任务。
```

# 

I'll start by locating the relevant files in the CBR-Addon-MachineMax project.

Now let me research MachineMax's vehicle identification. Let me explore both the latest fork and the vendored old version.

The old jar has a different package layout (`common.vehicle`, not `common.mech.vehicle`). Let me inspect the old jar's classes since that's the compile target.

Let me decompile the key classes. First, testing IDEA MCP access to the old jar.

IDEA MCP decompilation works. Now let me read the core vehicle identity classes.

Now let me read `VehicleCore` — this is where part entities get created.

Now let me check the CBR side: `IGameIdWriteApi` and the LootContext, plus the addon's own manager/event handler.

Let me examine how positions are applied — `Part` construction and `VehicleCore.setPos`.

Let me check whether the newer fork has a canonical "spawn vehicle" helper, which would clarify "默认状态".

Now a critical detail: when are part entities actually created? Let me check `DestroyableObject`/`DestroyableRigidObject` and `Part.addToLevel`.

Let me verify how templates get registered (key format) in the old jar.

Let me verify field visibility and the `ResourceLocation` API used by the project.

Research is complete. Let me record the plan and implement.

Build passes. Let me check existing memory before recording what I learned.

已完成。改动只落在 CBR-Addon-MachineMax，IDEA 全量构建通过。

## 1. 载具标识的调查结论

MachineMax 里"载具"分几层，只有一层适合做战利品配置的标识：

| 层次 | 标识 | 注册表 |
|---|---|---|
| 运行时实例 | `VehicleCore.uuid` | `ObjectManager.levelVehicles` |
| **载具类型** | **模板 ResourceLocation** | `MMDynamicRes.TEMPLATES` (`Map<ResourceLocation, VehicleData>`) |
| 展示包装 | 蓝图 / 装配体 ResourceLocation | `BLUEPRINTS` / `ASSEMBLIES`，各自用 `template` 字段指向模板 |
| 玩家自存载具 | 物品内联 `VehicleData` | `MMDataComponents.VEHICLE_DATA` |

模板来自内容包 `spark_modules/<pack>/<namespace>/templates/<name>.json`，注册 ID 就是 `<namespace>:<name>`（官方包即 `machine_max:jeep`、`machine_max:ae86` 等）。

**因此确定一个载具只需要一项信息：模板 ResourceLocation**，而 `vehicleLocation` 已经承载它。按 Rule 2 我没有新增任何字段——`VehicleDataProtocol` 和 `VehicleDataProtocol056` 保持原样。

## 2. 生成实现

[VehicleGenerator.java](../CBR-Addon-MachineMax/core/src/main/java/xiao/cbra/machinemax/common/loot/VehicleGenerator.java) 现在完全对齐 `VehicleBlueprintItem.use` 的路径：

```
MMDynamicRes.TEMPLATES.get(vehicleLocation)
  → new VehicleCore(level, data.withNewUUID(random), readAdditionalData)
  → vehicle.setPos(...) → ObjectManager.addVehicle(vehicle)
```

生成位置取方块顶面中心、载具包围盒底面贴合（`spawnOrigin.y + 1 - template.min.y`），与物品"底面贴合准星面"的算法一致。我**没有**复刻物品的碰撞预检——战利品刷新应该稳定产出载具，卡在狭窄处直接不生成反而更糟。如果你要这个预检，告诉我。

关于 part Entity 的 gameId：`SubPart.entity` 不是构造时创建的，而是下一个 level tick 由 `SubPart.postTick()` 懒创建。所以我在 `addVehicle` 之后显式 `refreshPartEntity()` 再 `addGameId`，避免这次写入落空。

## 3. 需要你确认的两件事

**A. `readAdditionalData` 我取了 `false`**（一行的事，改起来很便宜）：

- `false` = 蓝图语义，零件组装进度归零、不读耐久/连接器/子系统数据 ← 我选的，即"默认状态"
- `true` = 装配体语义，原样恢复完整状态，落地即可驾驶

依据是 `VehicleBlueprintItem` 用 `false`，fork 的 `BaseVehicleItem.restoreFullState()` 默认值也是 `false`，且协议里只有 `vehicleLocation` 一个字段、没有地方携带装配状态。但如果你的战利品本意是"能直接开走的车"，那应该是 `true`。

**B. 一个我没有解决的既有限制**：`MMPartEntity` 在区块卸载/重载时会被销毁重建（`VehicleCore.refreshAllPartEntities` 和 `SubPart.postTick` 都会重建），重建出来的实体**不带 gameId**。要覆盖这条路径得 hook MachineMax 的重建逻辑，超出本次范围，先报给你决定。
