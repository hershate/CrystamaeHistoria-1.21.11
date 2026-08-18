# 审计第 51 轮：配置文件损坏容错实测（部分损坏 + 致命损坏）

日期：2026-08-19
范围：真实启动观察两类配置损坏下的行为（快照→损坏→启动→还原）——
部分损坏（blocks.yml 坏条目）与致命损坏（generic-stories.yml 非法 YAML）。

## 结果一：部分损坏 → 六段校验链跳过降级 ✅

向 blocks.yml 追加三类坏条目（tier 非数字 / 非法故事类型 / 标量孤儿节），
并将真实条目 STONE 改名（顺带实证真实条目移除）。启动观察：

```
Ignoring a story with an invalid material -> STONE_CORRUPT_TEST
Ignoring a story with an invalid material -> ROUND51_BADTIER
Ignoring a story with an invalid material -> ROUND51_BADMAT
Whole section missing for story -> ROUND51_NOSECTION
```

- 四类坏条目全部**逐条跳过 + INFO 日志留痕**（r35 分析文档 §1.2 的
  fillBlockDefinitions 六段校验链**实机实证**）；
- 插件完整启用、服务器健康（Done）、0 异常——降级不失效。

## 结果二：致命损坏 → jar 默认值合并自愈 ✅（重要正向发现）

generic-stories.yml 写入非法 YAML（流序列语法破坏）。启动观察：

```
ERROR: Cannot load plugins\CrystamaeHistoria\generic-stories.yml
org.bukkit.configuration.InvalidConfigurationException ...
```

- `YamlConfiguration.loadConfiguration` 记录 ERROR+栈后返回空配置
  （非抛出）→ `updateConfig` 的 **jar 默认值合并（copyDefaults）将文件
  完整重建**（损坏的 2 行 → 715 行有效内容）并落盘；
- 插件随后**完整启用**：995 独特故事加载、启动计时正常、服务器 Done、
  0 插件异常——磁盘损坏/管理员误写不会使附属瘫痪（留 ERROR 日志线索）。

**含义**：ConfigManager 的默认值合并路径具备配置级自愈能力——此前仅在
代码层知晓该机制，本轮为运行时实证。对"长期无人值守稳定运行"目标是
重要正向证据。

## 验证

三个配置均从快照还原（715 行与原版一致）；world_r51 用毕删除；
server.properties 还原；业务端口 25565 未触碰。
