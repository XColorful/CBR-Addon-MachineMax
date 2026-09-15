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

# 

```
我补充了mixin环境，后续mixin写到cbramachinemax.mixins.json

我修改成了刷新的载具直接能开，并且测试过生成位置差不多正确，只是略微比地面高（如果有模型最低处的信息则可以替代现用的vehicleData.min.y

继续解决重建实体丢gameId的问题
```

# 

I'll look at the new mixin setup and how `MMPartEntity` survives chunk reload.

Let me check how `SubPart.variables` is used by MachineMax, to see if it's a safe place to carry the gameId.

Let me check what CBR exposes for reading game state and the gameId implementation.

Key finding: CBR stores entity gameId in `entity.getPersistentData()`, and there's a public read API. Let me check how to obtain it.

Let me verify the entity-unload mechanism so the fix rests on solid ground.

Now I'll write the mixin. Design: `SubPart.refreshPartEntity()` is the only place `MMPartEntity` is created, so I transfer the gameId from the outgoing entity to the new one there.

Now the real gate — Gradle, which also runs the mixin annotation processor.

Let me review the final state of my changes.

Let me settle whether the declared-but-absent `refmap` is harmful, by checking Mixin's actual behavior.

Let me check the actual template bounds to answer the spawn-height question concretely.

完成。改动是新增一个 mixin + 注册配置。

## 方案：把 gameId 从旧实体转移到新实体

我确认了 `SubPart#refreshPartEntity()` 是 **MachineMax 全代码库中唯一创建 `MMPartEntity` 的位置**（fork 里 grep `new MMPartEntity` 也只有这一处），所以它是唯一的收敛点。

而区块卸载的机制我查了 MC 源码确认：`PersistentEntitySectionManager.unloadEntity` 会对实体调用 `setRemoved(UNLOADED_TO_CHUNK)` —— 实体对象只是被**标记移除**，`SubPart.entity` 这个强引用仍然指着它，它身上的 `getPersistentData()`（CBR 存 gameId 的地方）原封不动。所以不需要任何额外的全局映射表，直接在重建前后做一次"搬移"：

- `HEAD`：从 `this.entity`（即将被顶替的旧实体）读出 gameId
- `TAIL`：写进 `this.entity`（新建的实体）

[SubPartMixin.java](../CBR-Addon-MachineMax/core/src/main/java/xiao/cbra/machinemax/mixin/SubPartMixin.java) 就是这个，注册在 [cbramachinemax.mixins.json](../CBR-Addon-MachineMax/core/src/main/resources/cbramachinemax.mixins.json)。

选这个方案而不是"用静态 Map 记 vehicle UUID → gameId"，是因为 SubPart 会跟着 Part 在**合并/拆分**载具时整体转移到新的 VehicleCore 上（`absorbVehicle` / `VehicleCore(level, uuid, partNet, oldVehicle)`），按 SubPart 自身的实体引用传递天然覆盖这些情况，按载具 UUID 建表则会在拆分出的新载具上断链；而且不需要生命周期管理，没有泄漏。

同时 `generateVehicle` 里的 gameId 写入要保留——首次创建时没有旧实体可继承，那里是 gameId 的唯一来源。

## 验证情况

Gradle `:core:compileJava`（`--rerun-tasks`）和 `:core:jar` 都通过，产物里确认有 `xiao/cbra/machinemax/mixin/SubPartMixin.class` 和 `cbramachinemax.mixins.json`。

**但我无法启动游戏，所以运行时还没验证。** 你测试时看两点：启动日志里应有 `Mixing SubPartMixin from cbramachinemax.mixins.json into ...SubPart`；然后对刷新出的载具做一次区块卸载/重载，检查部件实体的 `NeoForgeData.GameId` 是否还在。另外 `injectors.defaultRequire = 1`，所以如果目标方法对不上会直接崩而不是静默失效——这也意味着这个 mixin 只在旧版 MachineMax jar 上成立。

## 两个遗留点

**1. 服务器重启 / 维度重新初始化仍会丢。** `ObjectManager.loadVehicles` 是从维度存档里的 `VehicleData` 重建 `VehicleCore` 的，而 `VehicleData` 里没有 gameId 字段，此时 `SubPart.entity` 是 null，没有可继承的来源。要覆盖得把载具与 gameId 的对应关系也持久化（比如另存一个 level data attachment），不确定你是否需要——BR 对局通常不跨重启。

**2. `refmap` 声明指向一个不会生成的文件。** `cbramachinemax.refmap.json` 工程里没有配置 mixin 注解处理器，不会产出。我查了 Mixin 0.8.5 的 `MixinConfig`：读不到 refmap 只会打一条 warn（"Reference map '...' could not be read..."）然后退化成按字面名解析——而 NeoForge 1.21.1 用官方映射、Mixin 目标类也是官方映射编译的，字面名就是对的，所以功能上没问题。要消掉这条警告，删掉 `cbramachinemax.mixins.json` 里的 `"refmap"` 行即可（NeoForge MDK 的 mixins.json 也是不写 refmap 的）。我没动它，你决定。

## 关于生成高度

`vehicleData.min` 的来源是 `VehicleData(VehicleCore)` 里的 `vehicle.getAABB()`，它聚合的是各 SubPart 的 **物理碰撞包围盒**（`stateOf(body).getCachedBoundingBox()`），所以 `min.y` 是碰撞体最低点，不是视觉模型最低点。官方包里几个模板的实测值：

| 模板 | min.y | max.y |
|---|---|---|
| jeep | -1.428 | 1.981 |
| ae86 | -0.952 | 1.446 |
| szn | -1.069 | 1.516 |

`VehicleData` 里没有发布模型空间的下界，所以没有一个现成字段能替换 `min.y`。如果确实需要视觉最低点，得从模型的 cube/bone 数据（`ModelController.getOriginModel()` 那侧）自己算，那是另一件事了——要做的话告诉我。
