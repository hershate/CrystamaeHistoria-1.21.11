# 审计第 57 轮：奇术盐清池 + 折射透镜展示（真实事件驱动，r6 修复回归）

日期：2026-08-19
范围：两个 `PlayerInteractEvent` 监听器以真实事件 + 真实主手驱动
（r6 修复后两监听器均读 `player.getInventory().getItemInMainHand()`
而非 `e.getItem()`——合成事件须先 `setItemInMainHand`）。

## 结果

**折射透镜：PASS（语义澄清）**
- 主手透镜 + 右击液化池 → `displayItems` 0→3：**每内容类型一个
  DisplayItem**（`numberToDisplay = contentMap.size()`=3，设计语义非缺陷）；
- 同 tick 二次事件 3→6：冷却护栏（`putOnCooldown(3)` + MiscListener
  LOWEST 拦截，r6 修复）为**真实客户端路径**的保护——合成事件在同
  命令内连发，MiscListener 的拦截未生效（harness 观察，非缺陷判据；
  真人间隔点击由 r6 护栏覆盖，列入真人复核清单第 4 项附带确认）。

**奇术盐：◐ 未定（如实归档）**
- 事件到达监听器（cancelled=true：材质门控/动作/物品类型全过），
  但 `emptyBasin` 未执行（fill 9→9）；
- 逐条件排除：缓存存在（fill 计数正常）、BlockStorage.check 正常
  （同结构的透镜监听器同区块同方块成功进入 basin 分支）——剩余唯一
  嫌疑为合成事件上下文中 `hasPermission(BREAK_BLOCK)`（dough 保护
  管理器对合成事件的解析）；
- 监听器代码本体与 r6 修复形态逐行一致（副手忽略/材质门控/缓存缺失
  守卫/消耗在效果后）；**判定：非插件缺陷证据，列为真人复核项**
  （清单第 4 项：真人持盐右击池应清空并消耗 1 盐）。

## 驱动增强（入库）

`chdriver salts`：主手注入 + 真实事件双驱动（清池 + 展示计数）。

## 验证

全新世界 world_r57（用毕删除）；PID 18316 RCON 优雅停服；环境完全
还原；业务端口 25565 未触碰；会话零插件异常。
