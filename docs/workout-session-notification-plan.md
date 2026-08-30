# 训练常驻通知卡片 —— `:session` 新模块设计与实现计划

> 状态：**计划阶段（未开始实现）**
> 目标：开始运动时通过系统通知常驻一张卡片，显示当前锻炼的组数，并提供"跳转到添加一组页面"的快捷入口。
> 完成方式：每个任务项完成后，将其标记为 `[x]`（已解决）。

---

## 一、背景与现状

多模块 Android 项目（Kotlin + Compose + Hilt + Room + Navigation3）：

| 模块 | 职责 |
|---|---|
| `:app` | UI 层（页面、导航、主题），依赖其余全部模块 |
| `:database` | Room 数据库 + Repository 实现，依赖 `:repository` |
| `:repository` | Repository 接口 + 领域模型（纯接口层） |
| `:calendar` / `:chart` / `:settings` | 功能模块 |

与本次功能相关的关键事实：

- **数据模型**：`DailyWorkoutAction` 是"一组"（set）级别记录：`instant`（时间戳）+ `action`（动作，含名称/是否计时/负重/计数）+ `takenWeight` / `takenCount` / `takenDuration` + `note`。
- **写入与查询能力已具备**：`IDailyWorkoutRepository.addWorkoutAction()` 写一组；`getWorkoutOfDayFlow(user, day)` 按天拿当日全部组。
- **现状缺口**：没有任何"运动会话"概念（每组手工录入）；全项目没有任何 Notification / 前台服务 / 运行时权限代码；`User` 目前单用户（`getCurrentUser()` 自动建号）。
- **依赖方向**：`:app → :session → :repository`（新模块只依赖接口层，天然解耦）。

---

## 二、需求解读与设计决策

| 需求点 | 设计解读 |
|---|---|
| "常驻卡片" | 前台服务（FGS）+ ongoing 通知。只有 FGS 能在用户离开应用甚至进程被杀后稳定存活并保持数据同步 |
| "当前锻炼的组数" | 当前动作今天已完成的组数（= 今天该动作 `DailyWorkoutAction` 的条数），**从 repository flow 实时推导，不另存副本** |
| "快捷添加一次锻炼次数" | **改为跳转到现有"添加一组"页面**（`AddWorkoutAction` 路由），由用户在原表单中完成录入，而非通知直接插库 |
| "开始运动" | 用户显式点击入口（**首页 FAB 菜单第一项**，按会话状态切换"开始运动/结束运动"）→ 申请通知权限 → 启动前台服务 |
| "当前动作"如何确定 | v1 自动跟随最新一组：会话只持久化 `active` + `startedAt`，当前动作 = 今天最新一条记录的 action，组数 = 该动作今天条数 |

### 关键设计决策

1. **新建 `:session` 模块**，包名 `site.xiaozk.dailyfitness.session`，只依赖 `:repository`（接口与模型），不依赖 `:app` / `:database`。
2. **会话状态最小化**：DataStore 持久化 `active` / `startedAt` 两个字段；展示数据（组数、当前动作名）全部从 DB flow 推导，DB 是唯一事实来源。
3. **导航解耦（方案 A：接口注入）**：导航能力（NavKey、NavBackStack、MainActivity）全部留在 `:app`；`:session` 只定义 `WorkoutSessionNavProvider` 接口与 intent 常量契约，由 `:app` 用 Hilt 提供实现，避免循环依赖。
4. **Intent 捕获桥接为 SharedFlow**：`MainActivity` 捕获通知带来的 intent（`onCreate` / `onNewIntent`），投递到 `:app` 内 Hilt `@Singleton` 的 `NavIntentBus`（`MutableSharedFlow<Intent>`），`AppHost` 收集后映射为 `NavKey` 执行导航。选 SharedFlow 而非 StateFlow：导航是事件不是状态，避免粘滞重放导致重复导航。

### 完整链路（通知"添加一组"按钮）

```
通知按钮
 → Provider 生成的 PendingIntent（action=ACTION_ADD_SET，指向 MainActivity）
 → MainActivity.onNewIntent（存活时）/ onCreate（被杀后）
 → NavIntentBus.emit(intent)          ← SharedFlow 桥接点
 → AppHost 收集 → 按常量映射 NavKey
 → navBackStack.add(AddWorkoutAction) ← NavKey 仅在此出现
```

---

## 三、`:session` 模块设计

### 职责分层

```
┌────────────────────────────────────────────────┐
│  WorkoutSessionController   (Hilt Singleton)    │
│  对外唯一门面：start / finish / quickAddSet     │
│  暴露 StateFlow<WorkoutSessionState>            │
└──────────────┬─────────────────────────────────┘
               │ 订阅
┌──────────────▼─────────────────────────────────┐
│  WorkoutSessionStateMachine（纯逻辑，可测试）    │
│  组合 repository 的当日 flow → 推导展示状态      │
└──────┬──────────────────────┬──────────────────┘
       │                      │
┌──────▼──────┐      ┌────────▼────────┐
│ SessionStore│      │ WorkoutSession  │
│ (DataStore) │      │   Service(FGS)  │
│ 持久化active│      │ 通知构建/更新/   │
│ /startedAt  │      │ 取消 + 处理按钮  │
└─────────────┘      └─────────────────┘
```

### 对外 API 面

1. **`WorkoutSessionState`**（数据类）：`active`、`startedAt`、`currentActionName`、`currentActionId`、`setsDone`（当前动作组数）、`totalSetsToday`
2. **`WorkoutSessionController`**（接口 + Hilt 实现）：
   - `val state: StateFlow<WorkoutSessionState>`
   - `suspend fun start()`：标记会话开始并拉起前台服务
   - `suspend fun finish()`：结束会话、撤通知、停服务
   - ~~`quickAddSet()`~~：**已决定不实现**——v1 快捷入口改为跳转添加页（导航由 app 侧通过通知按钮 + NavIntentBus 完成，Controller 无需此方法）
3. **`WorkoutSessionService`**（`@AndroidEntryPoint` 前台服务）：
   - `startForeground` 发布 ongoing 通知；订阅 `controller.state` 驱动通知内容更新
   - 通知按钮通过 `PendingIntent.getActivity`（由注入的 `WorkoutSessionNavProvider` 提供）跳转；`FINISH` 结束按钮回投服务自身（`PendingIntent.getService`）
   - `START_STICKY`：进程被杀后自动重启并恢复通知
4. **`WorkoutSessionNavProvider`**（接口，`:session` 定义，`:app` 实现）：
   - `pendingIntentAddSet()`：跳"添加一组"页
   - `pendingIntentOpenToday()`：点通知主体进当日训练页
5. **`SessionIntents`**（常量对象）：intent action 常量（`ACTION_ADD_SET`、`ACTION_OPEN_TODAY`）与可选 extra key（预选 actionId）
6. **`SessionStore`**：DataStore 持久化 `active` / `startedAt`
7. **通知构建器**：`NotificationCompat.Builder` + 专用通知渠道（`IMPORTANCE_LOW`、无声音、`setOngoing(true)`、`CATEGORY_PROGRESS`）
8. **Hilt 模块**：`@InstallIn(SingletonComponent)` 提供 Controller / Store / 通知构建器绑定
9. **模块自带 AndroidManifest.xml**（manifest 合并器合入 app）：
   - 权限：`POST_NOTIFICATIONS`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_SPECIAL_USE`
   - 组件：`<service android:name=".WorkoutSessionService" android:foregroundServiceType="specialUse">` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 属性
     （注：targetSdk 36 下 `health` 类型要求同时拥有健康传感器类权限，本项目不采集传感器数据，改用 `specialUse`）

### 核心数据流

- **开始**：首页 FAB 菜单点"开始运动" → 运行时申请 `POST_NOTIFICATIONS`（API 33+）→ `startForegroundService(ACTION_START)` → 服务内 `controller.start()` → DataStore 写 `active=true` → `startForeground` 发布通知 → 收集当日 flow
- **展示**：当日 `DailyWorkout` flow 每发一版，状态机推导"最新动作名 + 该动作今日组数" → 通知更新
- **跳转添加**：通知按钮 → PendingIntent → MainActivity → NavIntentBus → AppHost → `navBackStack.add(AddWorkoutAction)`；用户在原表单录入 → 写库 → flow 重新发射 → 通知自动更新组数
- **结束**：通知"结束"按钮 → `controller.finish()` → DataStore 清状态 → 撤通知 + `stopSelf()`
- **进程被杀**：服务 `START_STICKY` 重启 → 读 DataStore 恢复会话 → 重新收集 flow 重建通知

---

## 四、`:app` 模块改动清单（仅接线）

1. `settings.gradle`：`include ':session'`；`app/build.gradle.kts`：`implementation(project(":session"))`
2. **`NavIntentBus`**（Hilt `@Singleton`）：内部 `MutableSharedFlow<Intent>`，配置 `replay=0`、`extraBufferCapacity=1`、`onBufferOverflow=DROP_OLDEST`，暴露 `emit()` / `collect()` 收口方法
3. **`WorkoutSessionNavProvider` 实现**（Hilt 绑定）：`@ApplicationContext` 构造 PendingIntent（`Intent(MainActivity)` + action 常量 + `FLAG_ACTIVITY_SINGLE_TOP or CLEAR_TOP` + `FLAG_IMMUTABLE` + 唯一 requestCode）
4. **`MainActivity`**：`onCreate` 与 `onNewIntent` 将 intent 投递给 `NavIntentBus`
5. **`AppHost`**：`LaunchedEffect` 收集 bus，按 `SessionIntents` 常量映射 `NavKey`，执行 `navBackStack.add(AddWorkoutAction)` / `TrainDay(today)`
6. **首页 `HostFab`**：FAB 菜单新增"开始运动/结束运动"项（`HomePageScaffold`/`HostFab` 透传会话状态与回调，`HomeWorkoutPageViewModel` 注入 `WorkoutSessionController`）；`ActivityResultContracts.RequestPermission` 申请通知权限，拒绝则 Snackbar 提示（不影响应用内手动加组）
7. （可选 v2）首页 `HomeWorkoutPage` 订阅会话状态显示小卡片

---

## 五、任务清单（每完成一步标记 `[x]`）

### M1 模块骨架与逻辑层

- [x] M1.1 创建 `:session` Gradle 模块（android library + hilt + ksp），加入 `settings.gradle`，`:app` 添加依赖
- [x] M1.2 定义 `WorkoutSessionState` 数据类与 `WorkoutSessionController` 接口
- [x] M1.3 实现 `SessionStore`（DataStore 持久化 `active` / `startedAt`）
- [x] M1.4 实现状态机：组合 `getWorkoutOfDayFlow` 推导"当前动作名 + 组数"
- [x] M1.5 实现 `WorkoutSessionController`（`start` / `finish`，Hilt 注入 + `StateFlow` 暴露）
- [x] M1.6 单元测试：状态机推导、start/finish 状态流转（8 个测试全部通过，`:app` 编译验证通过）

### M2 通知与前台服务

- [x] M2.1 通知渠道创建（`IMPORTANCE_LOW`、`setOngoing(true)`、`CATEGORY_PROGRESS`）
- [x] M2.2 通知构建器（通知主体文案 + 按钮：跳转添加、结束）
- [x] M2.3 `SessionIntents` 常量对象与 `WorkoutSessionNavProvider` 接口定义（含 `@BindsOptionalOf` 可选绑定与 `LaunchAppNavProvider` 回退实现）
- [x] M2.4 `WorkoutSessionService`（`@AndroidEntryPoint` + FGS + `START_STICKY` + 状态订阅刷新通知 + `ACTION_START`/`ACTION_FINISH` 入口 + 仅 active→inactive 迁移时停服）
- [x] M2.5 模块 AndroidManifest：权限 + service 声明（`foregroundServiceType="specialUse"` + subtype 属性）
- [x] M2.6 模拟器手工验证：渠道创建 ✅ / 常驻通知显示（ONGOING、NO_CLEAR、progress、2 按钮）✅ / 进程 kill -9 后 `START_STICKY` 自动重启并恢复通知（pid 4516→4593）✅ / 结束按钮移除通知并停服 ✅ / 组数内容随写库刷新（依赖 DB 数据，交由 M3.6 全链路验证）

### M3 app 接线（导航桥接）

- [x] M3.1 实现 `NavIntentBus`（Hilt `@Singleton`，SharedFlow 配置 `replay=0 / extraBufferCapacity=1 / DROP_OLDEST`）
- [x] M3.2 实现 `WorkoutSessionNavProvider`（app 侧 `AppSessionNavProvider`，构造指向 `MainActivity` 的 PendingIntent，首次引用 `AddWorkoutAction`；同时移除 `:session` 的 `@BindsOptionalOf` 与 `LaunchAppNavProvider`，改为硬绑定，缺绑定编译期报错）
- [x] M3.3 `MainActivity` 捕获 intent（`onCreate` 仅首次创建 + `onNewIntent`）投递 bus
- [x] M3.4 `AppHost` 收集 bus → 常量映射 NavKey → `navBackStack.add(AddWorkoutAction)` / `TrainDay(today)`（带栈顶去重）
- [x] M3.5 **首页 FAB 菜单**加"开始运动/结束"入口（会话激活状态切换文案与图标）+ `POST_NOTIFICATIONS` 运行时权限流程（拒绝 Snackbar 降级）
- [x] M3.6 全链路手工验证：`ACTION_ADD_SET` → 添加页 ✓ → 表单录入（啊啊啊2/哦哦哦/50Kg×12）→ 通知更新为"哦哦哦 · 1 sets done" ✓ → 首页 FAB"结束运动"点击后会话结束、菜单切回"开始运动" ✓；训练日志页无开始/结束按钮 ✓

### M4 健壮性

- [x] M4.1 进程被杀恢复：`START_STICKY` + DataStore 重建通知（真机/`adb shell am kill` 验证）
- [x] M4.2 ~~跨天重算~~ **决定忽略**（用户确认不做跨天自动重订阅；跨天场景由下次开始会话自然使用新日期，避免在 state 中引入周期性轮询）
- [x] M4.3 权限被拒降级：模拟器验证——权限对话框选 "Don't allow" → Snackbar 提示，应用内页面/数据正常（另注意：`pm revoke` 会直接杀进程，属系统行为非崩溃）
- [x] M4.4 快速连点通知按钮：连发 2 次 `ACTION_ADD_SET` → 仅入栈 1 次（栈顶去重 + SharedFlow 缓冲）；back 一次正常返回（首次 back 被键盘收起，属正常 UI 行为）
- [x] M4.5 会话期间应用内新增其他动作：添加"啊啊啊"一组后通知自动切换为 "啊啊啊 · 1 sets done"（当前动作跟随最新一组，组数按新动作重算）

### M5 打磨与发布

- [x] M5.1 字符串资源（中英）全部走 `strings.xml`（走查确认无硬编码）
- [x] M5.2 通知点击主体跳转当日训练页（`TrainDay(today)`）：`ACTION_OPEN_TODAY` 模拟验证 → 跳到当日训练日志页 ✓
- [x] M5.3 release 构建验证：R8 混淆 + 资源压缩构建通过（临时 keystore），安装模拟器冒烟——启动/开始会话/通知显示/kill -9 恢复/结束全部正常（Hilt 服务、DataStore、Navigation3 反射 restore 在混淆下无缺失规则）
- [x] M5.4 补充单元测试：抽取 `sessionActionToNavKey` 纯函数（AppHost 复用）并新增 4 个映射单测；app 模块启用 `testImplementation`
- [x] M5.5 更新 README / 截图（功能列表新增常驻通知卡片说明；截图未更新）

---

## 六、边界情况与风险

- **API 33+ 通知权限**：拒绝时优雅降级（无常驻卡片，但应用内加组照常）
- **targetSdk 36 前台服务类型**：`health` 类型需附带健康传感器权限，本项目改用 `specialUse`（`FOREGROUND_SERVICE_SPECIAL_USE` + subtype 属性），老系统自动忽略该声明
- **"当前动作"歧义**：v1 自动跟随最新一组；后续如需"切回 A 动作继续加组"，可在通知加"切换动作"按钮或提供会话页（新模块自带 Compose UI，仍保持解耦）
- **进程死亡重放**：系统重建 Activity 可能重放启动 intent，属可接受边缘情况，v1 不特殊处理
- **跨天会话**：**已决定忽略**（会话跨天时组数仍按开始日的记录展示；用户确认此边界不做处理，下次开始会话自然使用新日期）
- **未来多用户**：会话模块只依赖 `getCurrentUser()` 接口，不受影响

---

## 七、验收标准

- [x] 开始运动后出现常驻通知卡片，显示当前动作名称与今日组数（模拟器验证："哦哦哦 · 2 sets done"）
- [x] 点击通知"添加一组"直接进入现有添加一组页面（原表单）
- [x] 在应用内录入一组后，通知组数自动更新（"啊啊啊 · 1 sets done"）
- [x] 应用进程被杀后通知自动恢复，状态不丢失（debug 与 release 双验证）
- [x] 结束运动后通知消失
- [x] 拒绝通知权限时应用功能不受影响
- [x] 新功能全部代码位于 `:session` 模块（除 app 侧接线），`:app` / `:database` / `:repository` 无业务侵入
