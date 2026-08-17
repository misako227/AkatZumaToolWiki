# GPU Particle System 使用文档

本文档说明重构后的 GPU 粒子系统如何接入和使用。当前系统采用：

- `ParticleSystem`：调用方使用的管理入口。
- `ParticleEmitTask`：一个发射器配置，描述粒子如何生成。
- `GPUParticleSystem`：底层 GPU 实现，管理 SSBO、compute shader、active index 压缩和 indirect draw 渲染。
- `Particle SSBO`：保存长期粒子状态。
- `EmitJob SSBO`：保存本帧发射任务。

一般业务代码只需要使用 `ParticleSystem` 和 `ParticleEmitTask`。

---

## 1. 基本接入

### 1.1 创建粒子系统

在渲染管理类中创建一个 `ParticleSystem`：

```java
private ParticleSystem particleSystem;

public PostProcessing(Loader loader) {
    particleSystem = new ParticleSystem();
}
```

`ParticleSystem` 内部会创建 `GPUParticleSystem`，并初始化粒子 SSBO、发射任务 SSBO、VAO/VBO 和 shader。

---

### 1.2 注册发射器

通过 `emit(new ParticleEmitTask()...)` 注册发射器：

```java
particleSystem.emit(new ParticleEmitTask()
        .position(0f, -60f, 0f)
        .direction(0f, 1f, 0f)
        .speed(5f)
        .spread(0.5f)
        .life(3f)
        .gravity(0.1f)
        .size(1.5f, 1.5f, 0f)
        .color(0f, 1f, 0f, 1f)
        .endColor(1f, 1f, 0f, 0f)
        .shape(ParticleEmitTask.SHAPE_CIRCLE)
        .motion(ParticleEmitTask.MOTION_BALLISTIC)
        .rate(50)
        .duration(-1f));
```

这个发射器会持续发射粒子：

- 每秒 50 个粒子。
- 从绿色渐变到黄色透明。
- 弹道运动。
- 无限持续。

---

### 1.3 每帧更新和渲染

在 FBO 渲染阶段调用：

```java
float frameDelta = getParticleFrameDeltaSeconds();
particleSystem.updateAndRender(frameDelta, RenderSystem.getProjectionMatrix(), camera);
```

`frameDelta` 应该是真实的秒级帧间隔，不建议直接使用 `partialTick` 或 `Minecraft.getInstance().getFrameTime()`。当前项目中 `PostProcessing` 使用 `MathUtil.getClientTime(partialTick)` 计算前后两帧的客户端时间差，并限制最大步长，避免高帧率下粒子变快。

建议放在已有深度缓冲已经复制完成、目标 FBO 已绑定之后。例如当前项目里放在 `PostProcessing.addToBuffer()` 中，和 `FinalRender` 共用 `mainFBO`。

---

## 2. 发射器参数

`ParticleEmitTask` 使用链式配置。

### 2.1 位置和方向

```java
.position(x, y, z)
.direction(dx, dy, dz)
```

- `position`：世界坐标。
- `direction`：发射方向，内部会自动归一化。
- 如果方向长度接近 0，会默认使用 `(0, 1, 0)`。

---

### 2.2 速度、扩散、生命、重力

```java
.speed(5f)
.speed(5f, 0.5f)
.speedCurve(1.25f)
.spread(0.3f)
.life(3f)
.gravity(2f)
```

- `speed(float)`：兼容旧接口，起始速度和结束速度相同。
- `speed(startSpeed, endSpeed)`：速度随生命周期从起始速度插值到结束速度。
- `spread`：随机扩散强度，越大越散。
- `life`：粒子生命周期，单位为秒。
- `gravity`：弹道模式下的重力加速度，正数表示向下加速。
- `speedCurve`：速度插值曲线，`1` 为线性，大于 `1` 前期更慢，小于 `1` 前期更快。
- `reverseDirection(true)`：速度值仍为正数，只把运动方向反向；不要复用 `velocity.w`，它仍然保存总生命周期。

---

### 2.3 尺寸和旋转

```java
.size(sizeX, sizeY, rotation)
.sizeOverLife(startX, startY, midX, midY, endX, endY, midSizeTime)
.fixedSizeScale()
.fixedRotation(rotation)
.rotationSpeed(radPerSecond)
```

- `sizeX`：粒子 billboard 宽度。
- `sizeY`：粒子 billboard 高度。
- `rotation`：粒子自身基础旋转角度，单位为弧度。
- 默认只调用 `.size(...)` 时，GPU 会在基础旋转角上为每个粒子叠加 `0～2PI` 的随机角度，保持旧版随机朝向。
- `.fixedRotation(rotation)`：关闭每粒子的随机旋转，所有粒子只使用指定角度。
- `.startSize(x, y)` / `.midSize(x, y)` / `.endSize(x, y)`：分别设置出生、中间、结束尺寸。
- `.midSizeTime(ageT)`：设置中间尺寸出现的生命周期比例。
- `.sizeOverLife(...)`：一次设置三段尺寸和中间时间点。
- `.fixedSizeScale()`：关闭 GPU 出生时统一施加给三段尺寸的 `0.55～1.15` 随机倍率，严格使用调用值。
- `.rotationSpeed(radPerSecond)`：设置贴图 billboard 粒子生命周期内围绕自身中心的屏幕空间自旋速度，正数为逆时针；当前 `STAR_TEXTURE` shader 使用该值。
- 旧 `.size(...)` 会把三段尺寸全部设置为同一个值，现有调用保持固定尺寸行为。

这可以实现不规则四边形效果，例如横向拉长：

```java
.size(2.0f, 0.5f, 0f)
```

让纵向光效始终保持屏幕空间竖直朝上：

```java
.size(0.5f, 2.4f, 0f)
.fixedRotation(0.0f)
```

`fixedRotation(0.0f)` 固定的是 camera-facing billboard 的屏幕空间旋转角，因此长边沿屏幕 Y 轴朝上；它不会把 billboard 改成固定世界平面，也不会取消 billboard 面向相机的行为。

固定旋转复用现有 `renderParams.y`：CPU 写入 `1` 表示叠加随机旋转，写入 `0` 表示只使用任务指定角度。三段尺寸新增 `sizeParams` 和 `sizeControl` 两个 vec4，当前 `Particle SSBO` 与 `EmitJob SSBO` 均为 `52 floats`。GPU 出生阶段生成的 `0.55～1.15` 随机尺寸倍率会同时乘到三段尺寸，保持尺寸曲线比例一致。

---

### 2.4 颜色渐变

```java
.color(r, g, b, a)
.midColor(r, g, b, a)
.midColorTime(0.5f)
.endColor(r, g, b, a)
```

- `color`：出生颜色。
- `endColor`：死亡时颜色。
- shader 会根据生命进度做 `start -> mid -> end` 三段插值。
- 如果不设置 `midColor(...)`，系统会自动使用 start/end 的中间值，旧调用保持兼容。

示例：从红色渐变到黄色透明：

```java
.color(1f, 0f, 0f, 1f)
.endColor(1f, 1f, 0f, 0f)
```

---

## 3. 粒子形状

通过 `shape(...)` 设置：

```java
.shape(ParticleEmitTask.SHAPE_CIRCLE)
```

当前支持：

| 常量 | 说明 |
| --- | --- |
| `SHAPE_CIRCLE` | 圆形 |
| `SHAPE_SQUARE` | 方形 |
| `SHAPE_TRIANGLE` | 三角形 |
| `SHAPE_HEART` | 心形 |
| `SHAPE_STAR` | 五角星 |

形状在 fragment shader 中通过 SDF 裁剪实现，并带有边缘柔化。

---

### 3.1 SDF 与材质粒子的切换

默认不调用 `.material(...)` 时，粒子使用 `DEFAULT_SDF`，走 `SDF_BASIC` pipeline，并由 `.shape(...)` 决定几何形状。

```java
new ParticleEmitTask()
        .shape(ParticleEmitTask.SHAPE_STAR)
        .color(0xFFF59D, 0.92f)
        .endColor(0xFFB300, 0.0f);
```

需要三噪声光效时才调用材质：

```java
new ParticleEmitTask()
        .material(ParticleMaterialKey.LIGHT_EFFECT);
```

`ParticleEmitTask.material(...)` 会写入材质 ID，Compute Shader 再按材质表中的 pipeline id 把活跃粒子写入对应 active index。也就是说，不同粒子可以使用不同 shader，但实际切换发生在渲染阶段的 pipeline 批次，而不是逐粒子调用切换 shader。

---

## 4. 运动模式

通过 `motion(...)` 设置：

```java
.motion(ParticleEmitTask.MOTION_BALLISTIC)
```

当前支持：

| 常量 | 说明 |
| --- | --- |
| `MOTION_BALLISTIC` | 弹道运动，使用初速度和重力 |
| `MOTION_CIRCULAR` | 圆形/螺旋运动 |
| `MOTION_RADIAL_DIFFUSION` | 径向扩散运动，单个发射任务批量生成均匀向外炸开的圆形扩散 |
| `MOTION_DIRECTION_PLANE_RANDOM` | 方向平面随机运动，沿 `direction(...)` 主方向移动，并在垂直平面持续平滑随机摆动 |
| `MOTION_ARC_DIRECTION` | 专用弧面方向运动，从世界 +Y 同步旋向 `direction(...)` 目标方向，可配置劈落、保留和淡出时间 |
| `MOTION_TURBULENT_RISE` | 噪声流场上升运动，圆盘出生后沿主方向上升，并叠加 curl 噪声、低频风和生命周期径向扩散 |

---

### 4.1 弹道运动

```java
particleSystem.emit(new ParticleEmitTask()
        .position(0f, 64f, 0f)
        .direction(0f, 1f, 0f)
        .speed(4f)
        .spread(0.4f)
        .gravity(1.5f)
        .life(2.5f)
        .shape(ParticleEmitTask.SHAPE_CIRCLE)
        .motion(ParticleEmitTask.MOTION_BALLISTIC)
        .rate(80)
        .duration(2f));
```

适合火花、烟尘、喷射、爆裂碎片。

---

### 4.2 圆形/螺旋运动

圆形运动需要额外配置 `orbit(...)`。圆心使用 `position(...)` 传入的位置：

```java
.motion(ParticleEmitTask.MOTION_CIRCULAR)
.orbit(radius, angularSpeed, verticalSpeed)
```

参数说明：

- `radius`：轨道半径。
- `angularSpeed`：角速度，单位为弧度/秒。
- `verticalSpeed`：沿轨道平面法线方向的速度，非 0 时形成螺旋上升或下降。

圆形模式还支持控制粒子在圆周上的出生位置：

```java
.orbitPhase(angleRadians)
.orbitPhaseRandom(rangeRadians)
.orbitPhaseRange(startRadians, endRadians)
.orbitSpawnMode(ParticleEmitTask.ORBIT_SPAWN_RANDOM)
```

出生模式：

| 常量 | 说明 |
| --- | --- |
| `ORBIT_SPAWN_FIXED` | 固定从 `orbitPhase(...)` 指定角度出生 |
| `ORBIT_SPAWN_RANDOM` | 从 `orbitPhase(...) + 随机 * range` 的角度出生，默认整圈随机 |
| `ORBIT_SPAWN_RANGE` | 在 `orbitPhaseRange(start, end)` 指定弧段内随机出生 |
| `ORBIT_SPAWN_DISTRIBUTED` | 同一批粒子按数量均匀分布在圆周或指定范围内 |

圆形模式还支持设置轨道平面角度，让圆不再只在水平 `XZ` 平面旋转：

```java
.orbitPlane(pitchRadians, yawRadians, rollRadians)
.orbitPlaneRandom(pitchRangeRadians, yawRangeRadians, rollRangeRadians)
```

如果不设置 `orbitPlane(...)`，默认保持水平圆形轨道。设置后，圆周偏移会按 `pitch/yaw/roll` 旋转到世界空间，适合把星轨斜挂到天空方向。

`orbitPlaneRandom(...)` 会在每个发射任务写入 GPU 时给轨道平面增加一次随机偏移，适合让多批次星轨不完全重合。它不会扩大 SSBO，只是在 CPU 写入 `EmitJob` 时计算最终角度。

示例：

```java
particleSystem.emit(new ParticleEmitTask()
        .position(0f, 64f, 0f)
        .life(4f)
        .size(0.8f, 0.8f, 0f)
        .color(0.2f, 0.6f, 1f, 1f)
        .endColor(1f, 1f, 1f, 0f)
        .shape(ParticleEmitTask.SHAPE_STAR)
        .motion(ParticleEmitTask.MOTION_CIRCULAR)
        .orbit(5f, 2f, 1f)
        .orbitSpawnMode(ParticleEmitTask.ORBIT_SPAWN_RANDOM)
        .orbitPlane((float) Math.toRadians(60.0), 0f, 0f)
        .rate(100)
        .duration(-1f));
```

这个发射器会以 `position(0, 64, 0)` 为圆心，生成沿半径 5 的倾斜轨道旋转，并沿轨道法线方向移动的星形粒子。粒子会从圆周上的随机位置出生。
同一个圆心可以注册多个不同半径的发射器，形成多层星轨：

```java
Vec3 center = new Vec3(0.0, 100.0, 0.0);

particleSystem.emit(new ParticleEmitTask()
        .position(center)
        .motion(ParticleEmitTask.MOTION_CIRCULAR)
        .orbit(30f, 0.35f, 0f)
        .orbitSpawnMode(ParticleEmitTask.ORBIT_SPAWN_RANDOM)
        .orbitPlane((float) Math.toRadians(65.0), 0f, 0f)
        .orbitPlaneRandom((float) Math.toRadians(3.0), (float) Math.toRadians(8.0), 0f)
        .life(8f)
        .rate(160)
        .duration(-1f));

particleSystem.emit(new ParticleEmitTask()
        .position(center)
        .motion(ParticleEmitTask.MOTION_CIRCULAR)
        .orbit(55f, 0.22f, 0f)
        .orbitPhaseRange((float) Math.toRadians(20.0), (float) Math.toRadians(170.0))
        .orbitPlane((float) Math.toRadians(65.0), 0f, 0f)
        .life(10f)
        .rate(120)
        .duration(-1f));
```

实现说明：圆形模式复用了弹道模式下不使用的字段来传递出生角度和轨道平面角度；当前每粒子和每发射任务数据大小均为 `52 floats`。


---

### 4.3 专用弧面方向运动

`MOTION_ARC_DIRECTION` 用于稳定的长光柱劈落。它不移动粒子中心，而是把 `position(...)` 作为光柱根部，在 `particle_directed_light_effect.vsh` 中把长矩形从根部沿当前方向展开。

```java
new ParticleEmitTask()
        .position(origin)
        .direction((float) targetDir.x, (float) targetDir.y, (float) targetDir.z)
        .life((arcTicks + holdTicks + fadeTicks + graceTicks) / 20.0F)
        .sizeOverLife(width, length, width, length, width * 0.82F, length, 0.68F)
        .fixedRotation(faceRoll)
        .lightEffectMask(0.68F, 0.18F)
        .material(ParticleMaterialKey.DIRECTED_LIGHT_EFFECT)
        .arcDirection(length, arcSeconds, holdSeconds, fadeSeconds)
        .burst(1);
```

字段语义：

| 字段 / Builder | 说明 |
| --- | --- |
| `direction.xyz` | 最终目标方向，EX 咖喱棒使用玩家视野方向并做 Y 轴夹紧 |
| `arcDirection(length, arc, hold, fade)` | `length` 为光柱长度，`arc/hold/fade` 分别控制劈落、最终方向保留和淡出秒数 |
| `fixedRotation(faceRoll)` | 同轴补充面绕光柱长轴的旋转角，通常两片使用 `0` 和 `PI/2` |
| `position.xyz` | 光柱根部；顶点 Shader 使用根部 Pivot，因此长光柱不会半截落到玩家身后 |
| `life(...)` | 建议覆盖 `arc + hold + fade`，必要时额外加少量 grace，避免后续剑气粒子出现前光柱先消失 |

同一个发射 tick 内提交的多片光柱共享相同 `origin / targetDir / arc / hold / fade`，只改变 `faceRoll`，即可保持整根能量柱同步劈落。弧面方向模式会跳过普通 LIGHT_EFFECT 的通用生命周期淡入/淡出，改用 `arc + hold + fade` 时间轴控制透明度；顶点 Shader 会在根部额外预留端帽长度，片元 Shader 对该模式使用长轴椭圆/胶囊遮罩，让靠近玩家的一端不再被矩形几何切平成整齐截面。EX 咖喱棒开场光柱还会额外提交两片更窄、更淡的贴合式 V 形空气切痕；它们共享主光柱 `targetDir`，只通过起点边缘偏移和 `faceRoll` 倾斜贴在主光柱边缘，不再向左右分叉。


---

### 4.4 径向扩散运动

径向扩散用于地面冲击、爆炸扩散、流星落点扩散这类“从中心向外炸开”的效果。它保持批量提交能力：一个 `ParticleEmitTask` 可以用 `burst(...)` 一次生成一整圈扩散粒子，不需要一个粒子提交一个任务。

```java
particleSystem.emit(new ParticleEmitTask()
        .position(center)
        .speed(5.0f)
        .spread(0.08f)
        .life(1.2f)
        .gravity(0.02f)
        .size(0.08f, 0.08f, 0f)
        .color(0xA5D8FF, 0.95f)
        .endColor(0x7C4DFF, 0.0f)
        .shape(ParticleEmitTask.SHAPE_CIRCLE)
        .motion(ParticleEmitTask.MOTION_RADIAL_DIFFUSION)
        .radialDiffusion(0.18f, 0.08f, 0.06f)
        .rate(0)
        .duration(0f)
        .burst(64));
```

参数说明：

- `speed(...)`：径向向外速度。
- `spread(...)`：角度扰动，单位为弧度；越大越随机，越小越圆。
- `gravity(...)`：径向扩散后的弹道重力。
- `radialDiffusion(spawnRadiusJitter, verticalSpeed, verticalSpeedJitter)`：
  - `spawnRadiusJitter`：粒子从中心附近出生的半径扰动，避免所有粒子挤在同一点。
  - `verticalSpeed`：基础上抛速度，让扩散稍微离地。
  - `verticalSpeedJitter`：上抛速度随机扰动。
- `burst(...)`：一次扩散生成的粒子数量。

径向扩散默认在水平 `XZ` 平面向外扩散。也可以复用 `orbitPlane(...)` 和 `orbitPlaneRandom(...)` 设置扩散平面，让扩散贴合斜面或光束方向。

```java
particleSystem.emit(new ParticleEmitTask()
        .position(center)
        .motion(ParticleEmitTask.MOTION_RADIAL_DIFFUSION)
        .orbitPlane((float) Math.toRadians(35.0), 0f, 0f)
        .speed(4.0f)
        .spread(0.06f)
        .life(1.0f)
        .radialDiffusion(0.12f, 0.04f, 0.05f)
        .burst(48));
```

实现说明：径向扩散模式仍然不扩大 `Particle SSBO` 和 `EmitJob SSBO`。它复用 `EmitJob.direction.xyz` 传扩散平面角度，复用 `motion.yzw` 传出生半径扰动、上抛速度和上抛扰动。GPU 根据 `localIndex` 均匀分配圆周角度，再叠加小扰动，出生后按弹道速度向外运动。

---

### 4.5 方向平面随机运动

方向平面随机用于“沿一个确定主方向移动，同时在另外两个方向持续随机摆动”的光效。主方向由 `direction(...)` 决定，GPU 会自动计算垂直于主方向的两个侧向轴。

如果主方向是 `(0, 1, 0)`，随机摆动就落在 `XZ` 平面；如果主方向换成玩家视线方向，随机平面会跟着视线方向旋转。

```java
particleSystem.emit(new ParticleEmitTask()
        .position(center)
        .direction(0.0f, 1.0f, 0.0f)
        .directionPlaneRandom(0.28f, 0.9f, 0.75f)
        .speed(0.45f, 1.15f)
        .speedCurve(1.1f)
        .life(2.4f)
        .color(0xFFF6B0, 1.0f)
        .midColor(0xFFB238, 0.85f)
        .endColor(0xB02A08, 0.0f)
        .midColorTime(0.4f)
        .motion(ParticleEmitTask.MOTION_DIRECTION_PLANE_RANDOM)
        .burst(48));
```

参数说明：

- `directionPlaneRandom(amplitude, frequency, speed)`：设置侧向随机幅度、变化频率和时间推进速度，并自动切到 `MOTION_DIRECTION_PLANE_RANDOM`。
- `amplitude`：侧向随机最大幅度，越大越散。
- `frequency`：随机变化频率，越大轨迹越碎。
- `speed`：随机随时间推进速度，越大摆动越快。
- `speed(start, end)`：粒子速度从开始值插值到结束值。
- `speedCurve(power)`：控制速度插值曲线。
- `reverseDirection(true)`：只反转方向，不把速度写成负数。

实现上侧向随机使用 shader 内一维连续值噪声，不再用高频 `sin/cos` 直接叠加。`frequency` 和 `speed` 建议先保持在 `0.6 ~ 1.2` 区间；数值过高仍会让轨迹显得碎。

---

### 4.6 噪声流场上升运动

噪声流场上升用于“随机向上卷动”的能量雾、魔法火焰和 UE5 Niagara Curl Noise 风格光效。它和方向平面随机不同：方向平面随机按出生点绝对重算侧向 offset，而噪声流场上升每帧按速度场积分，粒子会持续漂移和翻卷。

```java
particleSystem.emit(new ParticleEmitTask()
        .position(center)
        .direction(0.0f, 1.0f, 0.0f)
        .turbulentRise(0.32f, 1.15f, 0.42f, 0.14f)
        .turbulentSpawnHeight(-0.5f, 2.0f)
        .speed(0.32f, 0.12f)
        .speedCurve(1.05f)
        .life(4.0f)
        .size(0.07f, 0.45f, 0.0f)
        .color(0xFF8FE6, 0.28f)
        .midColor(0xFFFFFF, 0.90f)
        .endColor(0xB7FFF4, 0.0f)
        .shape(ParticleEmitTask.SHAPE_STAR)
        .rate(70)
        .duration(3.0f));
```

参数说明：

- `turbulentRise(spawnRadius, noiseScale, curlStrength, radialExpansion)`：自动切到 `MOTION_TURBULENT_RISE`。
- `spawnRadius`：出生圆盘半径，决定底部宽度。
- `noiseScale`：噪声空间频率，越大局部变化越碎。
- `curlStrength`：局部卷曲速度强度，越大越容易形成翻卷。
- `radialExpansion`：生命周期后段向外扩散强度，控制顶部张开。
- `turbulentSpawnHeight(minOffset, maxOffset)`：设置粒子沿主方向的随机出生高度范围，偏移量相对 `position(...)`；不调用时默认 `-0.04～0.04`，保持旧版行为。
- `speed(start, end)`：主方向上升速度，通常结束速度低于开始速度，让顶部有悬停感。

噪声流场模式本身不额外增加字段；当前系统统一使用 `52 floats` 粒子和发射任务布局。该模式复用 `physics.y/w` 保存出生半径和径向扩散，复用 `motion.y/z/w` 保存 curl 强度、噪声频率和噪声时间速度，并复用 `renderParams.z/w` 保存出生高度最小/最大偏移。

如果要接近 `docs/0.0.6/1.jpg` 的分层效果，建议使用多组 emitter：底部蓝紫小半径、中部粉色高 curl、顶部青绿色大半径扩散。`testitem.addTestTurbulentRiseParticles(...)` 已提供三层 SDF 星形粒子预览。

大范围环境氛围可以直接增大 `spawnRadius`，同时保持较低上升速度和 curl。主方向为 `(0, 1, 0)` 时，粒子会在水平 `XZ` 圆盘内出生：

```java
particleSystem.emit(new ParticleEmitTask()
        .position(playerCenter)
        .direction(0.0f, 1.0f, 0.0f)
        .speed(0.12f, 0.04f)
        .speedCurve(1.10f)
        .turbulentRise(10.0f, 0.22f, 0.035f, 0.08f, 0.16f)
        .turbulentSpawnHeight(-0.75f, 6.0f)
        .life(8.0f)
        .size(0.05f, 0.05f, 0.0f)
        .color(0xFFF4B0, 0.16f)
        .midColor(0xFFD05A, 0.48f)
        .endColor(0x8A2E00, 0.0f)
        .shape(ParticleEmitTask.SHAPE_STAR)
        .rate(12)
        .duration(0.32f));
```

`spawnRadius = 10` 表示圆盘最大半径约 10 格，不是直径 10 格。`turbulentSpawnHeight(-0.75, 6.0)` 表示沿向上主方向从锚点下方 `0.75` 格到上方 `6` 格随机出生。大范围氛围应优先降低 `rate`、`size` 和 `curlStrength`，避免远处粒子过密或高速横移。

---

### 4.6 咖喱棒蓄力持续上升粒子速度

`ExcaliburChargeParticleEffects` 中的持续 SDF 和持续 `LIGHT_EFFECT` 都使用弹道模式向上发射，并按蓄力阶段共用固定速度。内层/外层只区分发射率、扩散、生命周期、尺寸和材质，不再根据目标高度、生命周期或高度倍率反算起始速度。

| 阶段 | 共用起始速度 | 共用结束速度 | 速度曲线 |
| --- | ---: | ---: | ---: |
| 基础阶段 | `1.00` | `0.70` | `1.15` |
| 增强阶段 | `2.20` | `1.65` | `1.15` |

对应 Java 参数为：

```java
BASE_CHARGE_RISE_START_SPEED
BASE_CHARGE_RISE_END_SPEED
BASE_CHARGE_RISE_SPEED_CURVE
ENHANCED_CHARGE_RISE_START_SPEED
ENHANCED_CHARGE_RISE_END_SPEED
ENHANCED_CHARGE_RISE_SPEED_CURVE
```

调参时优先修改阶段的 `START_SPEED` 和 `END_SPEED`：两者同时增大，整组粒子会上升更快；只增大 `END_SPEED`，粒子后段减速会变弱；`SPEED_CURVE` 只改变生命周期内的速度过渡，不改变起止速度。由于四组持续粒子共用阶段速度，不能再单独调 SDF 或 `LIGHT_EFFECT` 的上升速度；需要制造层次时应调整各组的 `spread`、`life`、`rate` 或尺寸。

周围氛围 SDF 使用 `MOTION_TURBULENT_RISE`，仍独立使用 `AMBIENT_SDF_SPEED_START = 0.12` 和 `AMBIENT_SDF_SPEED_END = 0.04`，本节的阶段速度不会影响它。

---

### 4.7 光效粒子材质与 Shader Bloom 调参

光效粒子使用新增材质：

```java
.material(ParticleMaterialKey.LIGHT_EFFECT)
```

它走独立 `LIGHT_EFFECT` pipeline，shader 复用金色螺旋的三张噪声贴图，并额外使用 `noise_092_128x` 做顶部圆形遮罩消散：

| 角色 | 贴图 |
| --- | --- |
| Noise1 | `t_fx_tile_0012` |
| Noise2 | `fx_noise015` |
| Noise3 | `tile_0137_moon` |
| Top Dissolve | `noise_092_128x` |

示例：

```java
particleSystem.emit(new ParticleEmitTask()
        .position(center)
        .direction(0.0f, 1.0f, 0.0f)
        .directionPlaneRandom(0.35f, 2.4f, 1.0f)
        .speed(0.05f, 0.10f)
        .color(0xFFF4A8, 1.0f)
        .midColor(0xFFB000, 0.82f)
        .endColor(0x5A0800, 0.0f)
        .size(1.22f, 2.42f, 0.0f)
        .fixedRotation(0.0f)
        .lightEffectMask(0.20f, 0.18f)
        .material(ParticleMaterialKey.LIGHT_EFFECT)
        .burst(3));
```

上例使用固定旋转，让纵向 LIGHT_EFFECT 始终朝上。若需要每个光效随机倾斜，删除 `.fixedRotation(...)` 即可恢复默认随机旋转。

`particle_light_effect.fsh` 内置了最终圆形遮罩，并在该遮罩上叠加顶部噪声消散。最终遮罩半径和柔边现在属于发射器级参数：

- `.lightEffectMask(radius, softness)`：同时设置遮罩半径和柔边。
- `.lightEffectMaskRadius(radius)`：只设置遮罩半径。
- `.lightEffectMaskSoftness(softness)`：只设置遮罩柔边。
- 默认值为 `radius=0.20`、`softness=0.18`，两个值都会限制在 `0.001～0.707`。
- 参数复用 `Particle/EmitJob.sizeControl.zw`，不同发射器可以使用不同遮罩，不增加当前 `52 floats` SSBO 大小。

`radius` 越大，完整可见核心区域越大；`softness` 越大，圆形边缘过渡越宽。`radius + softness` 接近 `0.707` 时，四边形角点也会逐渐可见。材质 `bloomParams.z` 仍只控制 halo 柔边，不是最终圆形遮罩柔边。

其余 Shader 调参入口是：

- `LIGHT_EFFECT_NOISE1_TILE`：Noise1 的 X/Y 平铺倍率，默认 `vec2(0.5, 0.5)`。
- `LIGHT_EFFECT_NOISE2_TILE`：Noise2 的 X/Y 平铺倍率，默认 `vec2(0.5, 0.5)`。
- `TOP_DISSOLVE_START_Y` / `TOP_DISSOLVE_FULL_Y`：顶部消散影响范围，当前为 `0.28` / `0.62`。
- `TOP_DISSOLVE_NOISE_TILE`：`noise_092_128x` 顶部消散噪声平铺倍率，当前为 `vec2(1.0, 1.0)`。
- `TOP_DISSOLVE_SCROLL_SPEED`：顶部消散噪声向上流动速度，当前为 `0.5`。
- `TOP_DISSOLVE_CUTOFF_LOW` / `TOP_DISSOLVE_CUTOFF_HIGH`：顶部消散噪声阈值，当前为 `0.94` / `1.0`。
- `TOP_DISSOLVE_STRENGTH`：顶部最大溶解强度，当前为 `1.0`。

Bloom 范围不再通过调用点放大粒子顶点控制，避免大四边形造成片元面积平方级增长。当前调参入口为：

- 默认 SDF 粒子：`shaders/gpu/gpushader.fsh` 中的 `BLOOM_CORE_STRENGTH`、`BLOOM_HALO_STRENGTH`、`BLOOM_HALO_RADIUS`、`BLOOM_EDGE_WIDTH`。
- 三噪声光效粒子：`ParticleMaterialRegistry` 中 `LIGHT_EFFECT` 的 `bloomParams`，以及 `particle_light_effect.fsh` 的 `energy` / `haloMask` / `topDissolveMask`。
- 全局扩散：`BloomRender` 现在是 `1/2` 近景 Bloom + `1/4` 远景 Bloom 加法回叠；近景调 `DEFAULT_SCREEN_BLUR_RADIUS` / `DEFAULT_ITERATIONS`，远景调 `FAR_BLOOM_SCALE` / `DEFAULT_FAR_BLUR_RADIUS` / `DEFAULT_FAR_ITERATIONS`。

更详细的参数说明见 `docs/0.0.6/4-1-2-shader-bloom-params.md`。

### 4.8 基础能量法阵材质

水平基础能量法阵使用独立材质：

```java
.material(ParticleMaterialKey.MAGIC_CIRCLE_ENERGY)
```

该材质走 `MAGIC_CIRCLE_ENERGY` pipeline，使用独立顶点 shader 将粒子固定展开在世界 `XZ` 平面，不再朝向相机。`size(x, y, rotation)` 中的 X/Y 尺寸分别对应世界 X/Z 方向，适合脚下能量波、水平法阵和地面范围提示。

材质使用两张按文件名命名的 atlas 纹理：

| 材质槽位 | 资源变量 | 用途 |
| --- | --- | --- |
| `baseTexture` | `tex_pattern66` | 最终能量脉络 |
| `noiseTexture0` | `tex_pattern59` | R 通道 UV 扰动 |

基础能量法阵和冲击波法阵统一调用片元 shader 的 `buildCircularSampleUv(...)`。该方法把局部 UV 中心化后计算单圈角度与线性半径；`tex_pattern59` 使用不乘 `noiseTileX/Y` 的平面 UV，只读取 R 通道并乘 `noiseStrength` 作为主纹理 UV 扰动。主纹理 UV 先由 `noiseTileX` 单独乘 X 分量、`noiseTileY` 单独乘 Y 分量，再交给 `sampleCircularBaseSeamless(...)`。双相位采样现在在平铺后的主纹理局部 U 上偏移 `0.5`，并根据 `fract(primaryUv.x)` 检测每个重复周期的接缝，在各周期边界的 `0.04` 窄范围内混合，避免 `noiseTileX = 2/5` 时遗漏中间周期接缝；其余圆周继续使用原主纹理。基础法阵当前主纹理 UV 平铺为 `(2,2)`，最终 `tex_pattern66.r` 直接乘圆形 mask、粒子 Alpha 和材质 Alpha 得到唯一 opacity；CA0 与 CA1 都基于该 opacity。

```java
particleSystem.emit(new ParticleEmitTask()
        .position(footAnchor)
        .direction(0.0F, 1.0F, 0.0F)
        .speed(0.0F, 0.0F)
        .spread(0.0F)
        .life(5.25F)
        .gravity(0.0F)
        .size(enhanced ? 10.50F : 7.50F, enhanced ? 10.50F : 7.50F, 0.0F)
        .color(enhanced ? 0xFFF7C4 : 0xFFE9A0, 0.18F)
        .midColor(0xFFB21A, enhanced ? 0.82F : 0.42F)
        .midColorTime(0.38F)
        .endColor(0x7A1900, 0.0F)
        .material(ParticleMaterialKey.MAGIC_CIRCLE_ENERGY)
        .motion(ParticleEmitTask.MOTION_BALLISTIC)
        .burst(1));
```

法阵本身速度保持 `0`，向外扩散来自 shader UV 动画，不需要让粒子在世界空间移动。咖喱棒当前每 `10 tick` 添加一个、生命周期 `5.25` 秒；基础阶段基准直径为 `7.50` 格，增强阶段为 `10.50` 格。Compute Shader 继续按当前 `0.55～1.15` 倍随机缩放每个新法阵，因此实际直径分别约为 `4.125～8.625` 格和 `5.775～12.075` 格。

纹理向外扩散速度由 `ParticleMaterialRegistry` 中 `MAGIC_CIRCLE_ENERGY` 材质的 `noiseSpeed` 控制，当前工作区值为 `0.35`。它只改变径向 UV 相位推进速度，不改变法阵的世界空间直径。

水平法阵 pipeline 绘制期间会临时关闭 `GL_CULL_FACE`，绘制结束后恢复进入前的剔除状态。因此同一个法阵粒子可以从上方和下方观察，不需要额外发射朝下副本；深度测试保持开启，贴地法阵从方块下方观察时仍会被方块正常遮挡。

`AkatZumaTextureAtlas` 在每次 atlas 上传完成后统一设置 `GL_LINEAR` 放大、`GL_LINEAR_MIPMAP_LINEAR` 缩小和最高 `4x` 各向异性过滤。该状态现在对整个自定义 atlas 生效，包含法阵、LIGHT_EFFECT、闪电和拖尾；法阵 pipeline 不再绑定独立 sampler。资源重载后会重新应用该设置，避免 F3+T 后恢复为 Point 过滤。线性过滤只改善纹理重建，不会增加 128x 纹理的真实细节，也不能替代 `textureGrad` 或透明边界抗锯齿。

详细参数位置、现值、调整顺序和故障对照见 `docs/0.0.6/基础能量法阵粒子.md`。

### 4.9 冲击波法阵材质

`SHOCKWAVE_MAGIC_CIRCLE` 与基础能量法阵共用 `MAGIC_CIRCLE_ENERGY` pipeline 和同一套水平法阵 Shader，不增加 draw pipeline：

```java
.material(ParticleMaterialKey.SHOCKWAVE_MAGIC_CIRCLE)
```

材质参数：

| 参数 | 值 |
|---|---:|
| materialId | `3` |
| pipelineId | `2` |
| `baseTexture` | `trail_2` |
| `noiseTexture0` | `tex_pattern59` |
| 平铺 | `(7,6)` |
| `noiseSpeed` | `3.0` |
| `noiseStrength` | `0.50` |

共享片元 Shader 继续读取主纹理 R 通道，因此冲击波法阵透明度来自 `trail_2.r`。它与基础能量法阵调用同一个 `buildCircularSampleUv(...)` 和 `sampleCircularBaseSeamless(...)`，同时使用 atlas 半 texel 内缩与按主纹理周期计算的双相位接缝混合；当前冲击波主纹理 UV 平铺为 `(5,3)`，其中 `5` 只作用于主纹理 U，`3` 只作用于主纹理 V，五个 U 周期的边界都会被检测；`tex_pattern59` 不使用这两个平铺值，最终圆形 mask 同时裁剪 CA0 和 CA1。

咖喱棒只在增强阶段提交该材质，基础阶段不会生成。冲击波法阵拥有独立的刷新、尺寸、生命周期、高度、速度和颜色参数。

冲击波法阵现已使用独立发射参数，不再依赖基础能量法阵：

| 参数 | 当前值 |
|---|---:|
| `SHOCKWAVE_MAGIC_CIRCLE_INTERVAL_TICKS` | `10` tick |
| `SHOCKWAVE_MAGIC_CIRCLE_LIFE` | `3.25` 秒 |
| `SHOCKWAVE_MAGIC_CIRCLE_Y_OFFSET` | `0.02` 格 |
| `SHOCKWAVE_MAGIC_CIRCLE_SIZE` | `14.00` 格 |
| `SHOCKWAVE_MAGIC_CIRCLE_SPEED` | `0.00` |
| 起始颜色/Alpha | `0xFFFDF0 / 0.18` |
| 中间颜色/Alpha | `0xE8D39A / 0.42` |
| 结束颜色/Alpha | `0x8A6A2B / 0.00` |
| `SHOCKWAVE_MAGIC_CIRCLE_MID_TIME` | `0.38` |

咖喱棒增强阶段按该材质自己的 `10 tick` 间隔生成，基准直径 `14.00` 大于基础能量法阵增强阶段的 `10.50`。`testitem` 右键方块也会调用正式 `emitShockwaveMagicCircle(...)`，用于观察相同参数的白金色冲击波法阵。

### 4.10 EX 剑气材质

EX 剑气使用独立材质和世界侧面平面 pipeline：

```java
new ParticleEmitTask()
        .position(center)
        .direction((float) sideNormal.x, (float) sideNormal.y, (float) sideNormal.z)
        .speed(0.0F, 0.0F)
        .spread(0.0F)
        .life(5.8F)
        .sizeOverLife(
                0.75F, 3.00F,
                5.00F, 18.00F,
                7.00F, 14.00F,
                0.85F)
        .fixedSizeScale()
        .fixedRotation(0.0F)
        .color(0xFF9E1A, 1.0F)
        .midColor(0xFF9E1A, 1.0F)
        .endColor(0xFF9E1A, 0.0F)
        .material(ParticleMaterialKey.EX_SWORD_WAVE)
        .burst(1);
```

`particle_ex_sword_wave.vsh` 把 `direction(...)` 写入的 `velocity.xyz` 当作侧面法线，并忽略 Y 分量保证四边形始终竖直。`.fixedRotation(...)` 控制的是该世界平面内围绕底边中心 Pivot 的手动旋转。CPU 侧会在 `ExcaliburSwordWaveEffects` 中继续对位置、尺寸、生命周期和颜色做稳定随机化，这里只给出正式基准值。

材质纹理槽位：

| 槽位 | 资源 |
| --- | --- |
| `baseTexture` | `ex_wave1` |
| `noiseTexture0` | `ex_wave2` |
| `noiseTexture1` | `noise_054` |

片元 Shader 现在把发射器的 `start/mid/end color` 作为 EX 剑气基础核心色。`ex_wave2.r` 使用该核心色，`ex_wave1.r` 按 `EX_WAVE1_HIGHLIGHT_SCALE` 生成高亮层；正式咖喱棒传入 `0xFF9E1A`，对应螺旋核心色约 `(1.00, 0.62, 0.10)`，高亮层约为螺旋 EDGE `(1.00, 0.92, 0.35)`。因此发射器传入其他 RGB 时，EX 剑气会实际跟随该色调，不再被 Shader 内绝对黄橙色主导。

两层 R 通道相加仍作为透明度。`noise_054.r * 0.1` 同时扰动两张主体纹理 UV；中心流速为 `(-0.5, 0.5)`，每个粒子根据稳定的 `p.extra.yz` seed 在 X/Y 方向增加 `±0.15` 随机速度。随机值在粒子生命周期内保持不变，不会逐帧抖动。

主要 Shader 调参入口：

- `NOISE_054_UV_SPEED`：所有粒子的中心 UV 速度。
- `NOISE_054_UV_SPEED_RANDOM_RANGE`：每粒子的独立 X/Y 速度随机范围。
- `NOISE_054_DISTORT_STRENGTH`：`noise_054.r` 对两张主体纹理的扰动强度。
- `EX_WAVE1_HIGHLIGHT_SCALE`：从发射器核心色生成第一层高亮色的 RGB 倍率，当前对齐咖喱棒螺旋 CORE/EDGE 色差。
- `EX_WAVE1_INTENSITY` / `EX_WAVE2_INTENSITY`：两层颜色强度。

`testitem` 右键方块当前调用 `addTestStarTextureParticle(...)`，提交一个 3 秒静止 `STAR_TEXTURE` 粒子，用于观察 `ai_star.png` 的 R 通道透明度、始终面向相机和 `.rotationSpeed(...)` 自旋。

---

### STAR_TEXTURE 星星贴图粒子

`STAR_TEXTURE` 是专用的材质贴图粒子，使用 `textures/atlases/ai_star.png`，并通过自定义 atlas 中的 `atlases/ai_star` sprite 采样。它不走 SDF 形状，也不使用 LIGHT_EFFECT 的三噪声链路。

透明度规则固定为：

```text
finalAlpha = texture(ai_star).r * lifecycleAlpha * materialAlphaScale
```

因此 PNG 的 R 通道黑色区域完全透明，白色区域由 `.color(...)`、`.midColor(...)` 和 `.endColor(...)` 传入的 alpha 控制；RGB 可见颜色仍来自发射器颜色。

示例：

```java
AkatZumaTool.POST.addParticle(new ParticleEmitTask()
        .position(center)
        .direction(0.0F, 1.0F, 0.0F)
        .speed(0.0F, 0.0F)
        .spread(0.0F)
        .life(3.0F)
        .gravity(0.0F)
        .sizeOverLife(3.20F, 3.20F, 4.60F, 4.60F, 5.20F, 5.20F, 0.28F)
        .fixedRotation(0.0F)
        .rotationSpeed(4.2F)
        .fixedSizeScale()
        .color(0xFFF4A8, 1.0F)
        .midColor(0xFFFFFF, 0.95F)
        .midColorTime(0.22F)
        .endColor(0xFF9E1A, 0.0F)
        .material(ParticleMaterialKey.STAR_TEXTURE)
        .motion(ParticleEmitTask.MOTION_BALLISTIC)
        .rate(0)
        .duration(0.0F)
        .burst(1));
```

`particle_star_texture.vsh` 使用普通 camera-facing billboard 展开，转动视角时不会变成侧面细线；`renderParams.z` 在该 shader 中解释为自旋速度，即 `rotation = fixedRotation + age * rotationSpeed`。`gpushader.comp` 中该槽位仍被 `MOTION_TURBULENT_RISE` 复用为出生高度下限，所以噪声上升模式不要同时依赖 `.rotationSpeed(...)`。

正式咖喱棒释放由 `ExcaliburSwordWaveEntity` 控制，不会一次性在最大射程内铺满粒子。客户端视觉从 `EX_WAVE_START_TICKS` 开始，用 `EX_WAVE_VISUAL_ADVANCE_TICKS`、`EX_WAVE_VISUAL_TRAVEL_TICKS` 和 `EX_WAVE_VISUAL_DISTANCE_POWER` 计算独立视觉距离，不再把 EX 粒子位置固定为每 tick 前进 `FORWARD_SPEED`；服务端伤害也改为独立的锥形体积推进，用 `EX_WAVE_DAMAGE_TRAVEL_TICKS`、`EX_WAVE_DAMAGE_DISTANCE_POWER` 和 `EX_WAVE_DAMAGE_PATH_KEEP_TICKS` 控制伤害锥前沿和旧路径保留：

```text
visualWaveAge = waveAge + EX_WAVE_VISUAL_ADVANCE_TICKS
visualTime = max(0, visualWaveAge - 1)
visualT = clamp(visualTime / EX_WAVE_VISUAL_TRAVEL_TICKS, 0, 1)
visualDistance = maxRange * pow(visualT, EX_WAVE_VISUAL_DISTANCE_POWER)
```

因此默认光柱落地后的第一批视觉粒子会从玩家位置开始，后续视觉速度由“最终距离 / 视觉总时间”动态反推；`EX_WAVE_VISUAL_ADVANCE_TICKS` 大于 0 时会让客户端粒子生成窗口提前并直接使用提前后的视觉进度，但服务端伤害判定仍按 `EX_WAVE_START_TICKS` 后的 `waveAge` 推进。

```text
damageTime = max(0, waveAge - 1)
damageT = clamp(damageTime / EX_WAVE_DAMAGE_TRAVEL_TICKS, 0, 1)
damageDistance = maxRange * pow(damageT, EX_WAVE_DAMAGE_DISTANCE_POWER)
retainedStartDistance = EX_WAVE_DAMAGE_PATH_KEEP_TICKS <= 0 ? 0 : damageDistance(waveAge - keepTicks)
```

客户端每 tick 根据当前视觉 V 字宽度计算动态奇数 `laneCount`，并在当前前沿路径上逐点位生成静止的 `EX_SWORD_WAVE` 主粒子、配套 billboard `LIGHT_EFFECT` 和后向 SDF 细节。视觉路线可通过 `EX_WAVE_VISUAL_LANE_SPACING` 和 `EX_WAVE_VISUAL_MAX_LANE_COUNT` 独立调节；`EX_WAVE_VISUAL_SIDE_SIGN` 只修正客户端左右方向，不改变服务端伤害路线：

```text
currentWidth = branchOffset(visualDistance) * 2
laneCount = ceil(currentWidth / EX_WAVE_VISUAL_LANE_SPACING) + 1
laneCount 至少为 3，偶数时加 1，最大为 EX_WAVE_VISUAL_MAX_LANE_COUNT
```

逐点位粒子基于实体视觉种子、客户端本地 tick、路线下标和参数盐生成稳定随机值；这些随机值只在出生时计算一次，粒子生命周期内保持稳定，位置扰动只影响客户端视觉，不改变服务端锥形伤害。

正式逐点位效果以 `START_SIZE_* / MID_SIZE_* / END_SIZE_*` 作为单粒子生命周期尺寸基准，再乘路径进度倍率 `EX_WAVE_PATH_START_SCALE -> EX_WAVE_PATH_END_SCALE`，实现玩家起点小、终点大：

```java
pathT = clamp(visualDistance / maxRange, 0, 1)
pathScale = lerp(EX_WAVE_PATH_START_SCALE, EX_WAVE_PATH_END_SCALE, pow(pathT, EX_WAVE_PATH_SCALE_POWER))
```

起点段因为 `pathScale` 较小，会额外调用 `emitStartFillSwordWaveParticles(...)` 沿同一条视觉路径补粒子。补粒子不再使用固定小步长堆在当前前沿附近，而是用上一批视觉前沿距离和当前视觉前沿距离计算动态间距，再把 `EX_WAVE_START_FILL_COUNT` 个粒子铺在两批前沿之间；补粒子生效距离通过 `resolveStartFillDistanceLimit(...)` 动态计算，取 `EX_WAVE_START_FILL_DISTANCE` 和第一段视觉步长 `1.25x` 的较大值，避免大射程时第二批剑气跳出固定补粒子范围。`EX_WAVE_START_EXTRA_FILL_TICKS` 和 `EX_WAVE_START_EXTRA_FILL_COUNT` 只增强起点前几批，让玩家身边前三批剑气更连续。`EX_WAVE_START_FILL_BACK_STEP` 保留为动态间距过小时的最小回退兜底，`EX_WAVE_START_FILL_SCALE_MIN/MAX` 控制补粒子的额外尺寸倍率。

长射程中段通过 `emitPathFillSwordWaveParticles(...)` 做通用路径补点。每 tick 先取上一批视觉距离和当前视觉距离，再用 `resolvePathFillCount(...)` 按 `EX_WAVE_PATH_FILL_SPACING` 自动计算补点数量，最大不超过 `EX_WAVE_PATH_FILL_MAX_COUNT`；补点沿同一 lane 在两批视觉前沿之间均匀插入，只生成 `EX_SWORD_WAVE` 主粒子和配套 `LIGHT_EFFECT`，不额外生成后向 SDF。`EX_WAVE_PATH_FILL_START_AGE` 控制从第几批视觉剑气开始启用中段补点，`EX_WAVE_PATH_FILL_SCALE_MIN/MAX` 控制补点尺寸随机倍率。

每一路稳定随机范围：

| 参数 | 当前范围 |
| --- | ---: |
| 沿 forward 位置扰动 | `-0.35～0.35` 格 |
| 沿 side 位置扰动 | `-0.45～0.45` 格 |
| 底边 Y 扰动 | `-0.18～0.22` 格 |
| 侧面法线水平偏转 | `-12°～12°` |
| 平面内底边 Pivot 旋转 | `-6°～6°` |
| 主粒子生命周期倍率 | `0.88～1.12` |
| 颜色亮度倍率 | `0.90～1.08` |
| 路径整体尺寸倍率 | `EX_WAVE_PATH_START_SCALE～EX_WAVE_PATH_END_SCALE` |
| 配套 LIGHT_EFFECT Y 偏移 | `EX_WAVE_LIGHT_Y_OFFSET` |
| 配套 LIGHT_EFFECT 出生 Alpha 倍率 | `0.30～0.45` |
| 配套 LIGHT_EFFECT 中段 Alpha 倍率 | `0.25～0.40` |

后向 SDF 细节仍按 `15%/60%/25%` 概率生成 `0/1/2` 个三角/方形/星形碎片，沿水平后方、随机左右侧向和少量上方向飞散。`.fixedSizeScale()` 继续关闭 Compute Shader 的 `0.55～1.15` 默认随机尺寸。

开场能量光柱由 `ExcaliburSwordWaveEffects.emitOpeningDirectedLightColumn(...)` 在实体第 1 tick 一次性提交。主光柱使用多片首尾一致、绕长轴错开的 `DIRECTED_LIGHT_EFFECT + MOTION_ARC_DIRECTION` 长粒子，从世界 +Y 旋向玩家视野目标方向；两侧空气切痕不再偏移 `targetDir` 往外分叉，而是共享主光柱目标方向，只通过起点沿 `side` 的边缘偏移和 `faceRoll` 倾斜贴在主光柱边缘，表现为劈开空气的淡 V 形切痕。`resolveLightColumnTargetDirection(...)` 对小于 `LIGHT_COLUMN_SMALL_ARC_DEGREES` 的小角度劈砍启用保护，避免被 `LIGHT_COLUMN_TARGET_MAX_Y` 强行扩大劈落角度。`LIGHT_COLUMN_LIFE` 包含 `LIGHT_COLUMN_SLASH_TICKS + LIGHT_COLUMN_HOLD_TICKS + LIGHT_COLUMN_FADE_TICKS + LIGHT_COLUMN_GRACE_TICKS`，其中 grace 只用于生命周期缓冲，避免 EX 剑气粒子出现前光柱提前消失。

光柱劈落阶段额外调用 `emitLightColumnFanSdfParticles(...)`，在 `LIGHT_COLUMN_SLASH_TICKS` 内每 tick 沿当前弧面方向采样 `LIGHT_COLUMN_FAN_SDF_PER_TICK` 个短寿命 `DEFAULT_SDF` 粒子。扇面 SDF 的半径、侧向宽度、尺寸、透明度、寿命、速度和旋转由 `LIGHT_COLUMN_FAN_SDF_*` 参数组控制，用于表现整片劈砍弧面里的空气碎光。

最大视觉路线数由 `EX_WAVE_VISUAL_MAX_LANE_COUNT` 控制；起点段会按 `EX_WAVE_START_FILL_COUNT` 和起点额外补粒子参数补充同路径主粒子和配套 `LIGHT_EFFECT`，长射程中段会按 `EX_WAVE_PATH_FILL_*` 补充前后方向路径点，并按概率追加后向 SDF 细节；光柱劈落阶段再额外提交扇面 SDF。默认仍低于 `MAX_EMIT_JOBS=768`，本次未修改 `MAX_PARTICLES`，仍保持 `100000`，也未增加新的 Render Pipeline。

`setting.exExcalibur` 配置控制正式技能的 `maxRange`、`branchDistance`、`damage`、`damageHeightUp`、`damageHeightDown` 和 `damageSidePadding`。伤害只在服务端按完整 V 字锥形体积计算，不使用 GPU 粒子参与碰撞；左右半宽由 `branchDistance * progress + damageSidePadding` 决定，其中 `branchDistance` 继续控制视觉 V 字展开，`damageSidePadding` 专门让服务端伤害覆盖主粒子、配套光效和随机扰动形成的可见边缘；上下范围由中心线附近 `[-damageHeightDown, +damageHeightUp]` 决定。每 tick 对目标中心、眼睛、脚底和顶部代表点做锥形精筛，命中后清除原版受伤无敌帧，让旧锥形路径保留期间可以持续造成伤害。

EX 顶点 Shader 使用底边中心 Pivot：`aPos.y = -0.5` 对应局部高度 `0`，尺寸变化只向上展开，不再从粒子中心向下延伸半个高度。正式效果在客户端起点位置上额外应用 `PARTICLE_BASE_Y_OFFSET = -1.45`，让底边从玩家脚边附近开始；该偏移不参与服务端伤害检测。

GPU 粒子在 `DEPTH_TESTED_WORLD` 阶段依赖 `mainFBO` 中复制的场景深度。Minecraft RenderType 的 `endBatch()` 可能在清理状态时关闭真实 `GL_DEPTH_TEST`，因此 `PostRenderContext.setDepthState(...)` 每次调用都必须写入真实 GL 状态，且 `PostProcessing` 在 `particleSystem.updateAndRender(...)` 前会再次设置：

```text
GL_DEPTH_TEST = Enabled
GL_DEPTH_FUNC = GL_LEQUAL
GL_DEPTH_WRITEMASK = False
Draw Buffers = CA0 + CA1
```

实体的 `shouldRender=true` 和 `noCulling=true` 只控制是否调用 Renderer，不会让 GPU 粒子绕过场景深度。
---

## 5. 发射模式

### 5.1 持续发射

```java
.rate(100)
.duration(3f)
```

- `rate`：每秒发射数量。
- `duration`：持续时间，单位为秒。
- `duration(-1f)` 表示无限持续。

`ParticleSystem` 内部会用累积器处理非整数帧发射量，避免不同帧率下发射数量明显不一致。

---

### 5.2 单次爆发

```java
.burst(200)
```

`burstCount` 只会触发一次。可以和 `rate(...)` 同时使用：

```java
particleSystem.emit(new ParticleEmitTask()
        .position(0f, 64f, 0f)
        .direction(0f, 1f, 0f)
        .speed(8f)
        .spread(1.2f)
        .life(1.5f)
        .size(0.4f, 0.4f, 0f)
        .color(1f, 0.3f, 0f, 1f)
        .endColor(1f, 1f, 0f, 0f)
        .shape(ParticleEmitTask.SHAPE_CIRCLE)
        .motion(ParticleEmitTask.MOTION_BALLISTIC)
        .burst(200)
        .duration(0f));
```

适合一次性爆炸、命中特效。

---

## 6. 多发射器

可以同时注册多个发射器：

```java
particleSystem.emit(fireTask);
particleSystem.emit(smokeTask);
particleSystem.emit(sparkTask);
```

每帧流程：

1. `ParticleSystem` 遍历所有 `ParticleEmitTask`。
2. 计算每个发射器本帧应发射数量。
3. 写入 `EmitJob SSBO`。
4. `GPUParticleSystem` 执行一次 `dispatchCompute`。
5. vertex/fragment shader 渲染所有粒子。

底层默认最多支持：

| 项 | 当前值 |
| --- | --- |
| 最大粒子数 | `100000` |
| 单帧最大发射任务数 | `768` |
| 每粒子数据大小 | `52 floats` |
| 每发射任务大小 | `52 floats` |

如果某一帧超过 `MAX_EMIT_JOBS`，超出的发射任务会被忽略。

---

## 7. 资源释放

如果粒子系统生命周期结束，应调用：

```java
particleSystem.cleanUp();
```

它会释放底层 VBO、VAO、SSBO 和 shader program。

当前 `PostProcessing.cleanUp()` 里还没有真正调用实例的 `particleSystem.cleanUp()`，如果后续补完整生命周期管理，需要把它接进去。

---

## 8. 注意事项

- `ParticleEmitTask` 是有运行时状态的对象，包含 `elapsed`、`emitAccumulator`、`burstEmitted` 等字段。一个 task 注册后不要重复注册到多个系统里复用。
- `.size(..., rotation)` 的 `rotation` 默认是随机角度的基础偏移；要求严格固定朝向时必须额外调用 `.fixedRotation(rotation)`。
- `duration >= 0` 的发射器到期后会从 `ParticleSystem` 中移除，但已经生成的粒子会继续在 GPU 中活到自己的 `life` 结束。
- 当前透明混合使用 `GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA`，没有做透明粒子排序。大多数特效可以接受，但严格透明遮挡不会完全正确。
- 当前渲染已经使用 active index SSBO 按 pipeline 压缩活跃粒子，并用 indirect draw 的 instanceCount 绘制实际活跃数量，不再每个 pipeline 固定绘制 `MAX_PARTICLES`。
- `GPUParticleSystem.setEmitter(...)` 只是兼容旧接口，新代码建议统一使用 `ParticleSystem.emit(new ParticleEmitTask()...)`。

---

## 8.1 定向 LIGHT_EFFECT

`ParticleMaterialKey.DIRECTED_LIGHT_EFFECT` 复用普通 `LIGHT_EFFECT` 的三噪声贴图、顶部消散、圆形遮罩和 Bloom 片元逻辑，但使用独立 `ParticleRenderPipeline.DIRECTED_LIGHT_EFFECT` 与 `particle_directed_light_effect.vsh`。

语义：

| 字段 | 用法 |
| --- | --- |
| `direction.xyz` | 普通定向光效表示世界空间光效平面法线；`MOTION_CIRCULAR` 下由圆形模式复用为轨道平面欧拉角；`MOTION_ARC_DIRECTION` 下表示最终目标方向 |
| `fixedRotation(...)` | 普通定向光效表示面内旋转；`MOTION_CIRCULAR` 和 `MOTION_ARC_DIRECTION` 下表示同轴补充光片绕长轴的旋转角 |
| `sizeOverLife(...)` | 继续控制出生 / mid / 结束三段宽高 |
| `lightEffectMask(...)` | 普通定向光效继续控制圆形遮罩半径和柔边；`MOTION_ARC_DIRECTION` 下控制长轴椭圆/胶囊遮罩 |

示例：

```java
new ParticleEmitTask()
        .position(position)
        .direction((float) planeNormal.x, (float) planeNormal.y, (float) planeNormal.z)
        .speed(0.0F, 0.0F)
        .spread(0.0F)
        .life(0.34F)
        .gravity(0.0F)
        .sizeOverLife(0.10F, 2.80F, 0.24F, 5.60F, 0.08F, 2.20F, 0.35F)
        .fixedSizeScale()
        .fixedRotation(rotation)
        .lightEffectMask(0.50F, 0.16F)
        .color(0xFFFFFF, 0.62F)
        .midColor(0xFFC247, 1.0F)
        .endColor(0x6A0900, 0.0F)
        .material(ParticleMaterialKey.DIRECTED_LIGHT_EFFECT)
        .motion(ParticleEmitTask.MOTION_BALLISTIC)
        .rate(0)
        .duration(0.0F)
        .burst(1);
```

`DIRECTED_LIGHT_EFFECT` 现在同时支持 `MOTION_CIRCULAR` 和 `MOTION_ARC_DIRECTION`。EX 咖喱棒开场光柱使用 `MOTION_ARC_DIRECTION`：Compute 保持根部位置和目标方向，顶点 Shader 按生命周期从世界 +Y 旋向目标方向，达到最终方向后保留并淡出。只需提交 2 个首尾一致、`fixedRotation` 不同的超长定向光效粒子，即可形成同步劈下的同轴多面能量柱。该模式的 fragment shader 会按 `vMotionType` 切换到长轴椭圆/胶囊遮罩，顶点 shader 额外扩展端帽几何，避免长粒子近端出现平直切面。

---

## 9. 推荐调用模板

```java
public class MyParticleOwner {

    private final ParticleSystem particleSystem = new ParticleSystem();

    public void init() {
        particleSystem.emit(new ParticleEmitTask()
                .position(0f, 64f, 0f)
                .direction(0f, 1f, 0f)
                .speed(5f)
                .spread(0.3f)
                .life(3f)
                .gravity(1f)
                .size(1f, 1f, 0f)
                .color(1f, 0f, 0f, 1f)
                .endColor(1f, 1f, 0f, 0f)
                .shape(ParticleEmitTask.SHAPE_CIRCLE)
                .motion(ParticleEmitTask.MOTION_BALLISTIC)
                .rate(100)
                .duration(-1f));
    }

    public void render(float dt, Matrix4f projectionMatrix, Camera camera) {
        particleSystem.updateAndRender(dt, projectionMatrix, camera);
    }

    public void cleanUp() {
        particleSystem.cleanUp();
    }
}
```
