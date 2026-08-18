# 审计第 39 轮：命令层对抗性审计（op + 非 op 双机器人实测）

日期：2026-08-18
范围：`HistoriaCommand` 分派器 + 5 个子命令（test-spell/test-wand/spells/
stories/rank）的对抗性审阅与实机测试——聊天命令是网络直连面，
覆盖"未特权用户 + 畸形参数"主线。

## 实测矩阵（端口 25599，双 bot：AuditOp39[op] / AuditPlain39[无权限]）

| 用例 | 结果 |
|------|------|
| `/ch` 帮助（op：5 命令 / 非 op：仅 3 免权限命令） | ✅ 权限过滤正确，无越权泄露 |
| `/ch rank`（非 op 可用） | ✅ 正常输出等级 |
| `/ch test-spell PUSH 3`（合法 op 施法） | ✅ 无异常 |
| `/ch test-spell PUSH 99`（越界）/ `PUSH abc`（非数字） | ✅ 统一守卫文案 |
| `/ch test-spell push 3`（大小写不符） | ✅ "法术不存在或无法释放" |
| `/ch test-spell PUSH`（缺参） | ✅ 静默返回（args.length 守卫） |
| `/ch test-wand ABSTRACT_VOID 1`（合法 id） | ✅ **法杖正确发放**（修正基准后复测 GRANTED=true） |
| `/ch test-wand AirNova 1`（类名形） | ✅ 优雅拒绝 |
| `/ch test-wand PUSH 5`（等级越界） | ✅ 静默返回 |
| `/ch nonsense`（未知子命令） | ✅ 提示 + 帮助 |
| `/ch test-spell`（非 op） | ✅ "你没有权限使用该命令" |
| 全会话服务端日志 | ✅ 0 命令异常 / 0 ERROR / 0 SEVERE |

## 诚实更正：eca4568 的缺陷声明为误报（本节为正式记录）

初判"法术 id(ABSTRACT_VOID) 与枚举常量名(AbstractVoid) 不同形 → 原
`SpellType.valueOf(args[0])` 必抛 IAE"——**错误**。该判断基于**文件名与 id
比对**的错误方法论：`SpellType` 枚举常量名实际就是 SCREAMING_SNAKE 形
（`ABSTRACT_VOID(new AbstractVoid())`），程序化比对确认**全部 69 个枚举名
与 id 完全一致**——原 `valueOf` 在该不变量下安全，原 Tab 补全
（`spell.name()` ≡ id）同样正确。实测中"法杖未发放"的假象为测试脚本将
计数基准误置于授予之后。

**处置**：保留 eca4568 的改动（按 id 循环解析，与原行为完全等价），但将其
定位从"修缺陷"更正为**防御性解耦**——id≡枚举名的不变量无任何强制约束，
按 id 解析消除对隐式耦合的依赖（未来 id 与枚举名分歧时不再抛 IAE）。
代码注释已同步更正（含误报缘由），Tab 补全改 `getId()` 与原 `name()`
等价。方法论教训入档：**枚举常量名必须从枚举声明取证，不能从文件名推断**。

## 验证

两轮服务器会话（PID 48244/39148）均 RCON 优雅停服、配置还原、端口释放、
业务端口 25565 未触碰；日志零异常。
