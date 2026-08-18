# 审计第 53 轮：Artistic 画笔消耗链驱动验证

日期：2026-08-19
范围：BasicPaintbrush（黑色 100 次款）消耗链——`tryPaintBlock` 生产
路径（PaintProfile 涂色语义）+ `LimitedUseItem.damageItem` 的 PDC 衰减
与耗尽损坏语义。

## 结果：PASS

| 断言 | 结果 |
|------|------|
| 涂色语义（WHITE_WOOL → BLACK_WOOL 族匹配） | ✅ 100/100 次涂色成功（首次修正：STONE 非 wool/terracotta/concrete 族不可涂——与实现语义一致） |
| PDC 使用衰减（uses 键，LimitedUseItem 机制） | ✅ 每次涂色后递减 |
| **耗尽语义（100 次后堆清空）** | ✅ `brush_depleted_at=100`——第 100 次 damageItem 后堆变空（AIR），与 LimitedUseItem "用尽即毁" 设计一致 |
| 零插件异常 | ✅（中途 NPE 为我驱动对空堆读 meta 所致，加守卫后即耗尽信号；非插件路径） |

## 附注

- `damageItem` 为 protected——跨类加载器同包不授予（r37 教训复用反射）；
- 画笔族交互的完整 GUI/实体涂色分支（Shulker/Sheep/Parrot/Axolotl）
  已由代码复核（r11）+ 本轮方块分支实证覆盖。

## 驱动增强（入库）

`chdriver brush`：涂色-衰减-耗尽全链驱动（含耗尽报告）。

## 验证

全新世界 world_r53（用毕删除）；三次会话均 RCON 优雅停服；环境完全
还原；业务端口 25565 未触碰。
