# [PECA] Player Extend Carpet Addition
![issues](https://img.shields.io/github/issues/FengLiuFeseliud/Player-Extend-Carpet-Addition-Mod)![forks](https://img.shields.io/github/forks/FengLiuFeseliud/Player-Extend-Carpet-Addition-Mod)![stars](https://img.shields.io/github/stars/FengLiuFeseliud/Player-Extend-Carpet-Addition-Mod)![license](https://img.shields.io/github/license/FengLiuFeseliud/Player-Extend-Carpet-Addition-Mod)

这是一个对 [地毯 carpet mod](https://github.com/gnembon/fabric-carpet) 假人方面的**扩展**

扩展了 **carpet** 多假人操作, 添加假人组控制, 假人队形, 假人保存/管理, 与各种假人特性

## 指令
### /playerGroup
使用此指令进行多假人操作, 进行多假人操作前必须创建一个组

使用此指令创建一个组 `/playerGroup [组名] spawn [假人数] `成功将召唤名称为 `组名 _ 假人编号的 ` 的假人, 数量为你输入的假人数

其它指令与 `/player` 一致, 不过填写的假人名变为组名, 并且组内所有假人都将执行, 唯一不一样的是 `stop`

`/playerGroup [组名] stop` 可以使用 `from ...假人编号 to ...假人编号` 来控制停止的组内假人范围

`from` 代表从哪里开始停止, 不填写 `to` 将一直停止到最后一个, `to` 代表从哪里结束停止

#### 例子
```markdown
# bot 组内有 10 名假人, 停止所有假人
/playerGroup bot stop
# bot 组内有 10 名假人, 从假人 5 停止到假人 10 (最后一个)
/playerGroup bot stop from 5
# test 组内有 10 名假人, 从假人 3 停止到假人 8
/playerGroup test stop from 3 to 8
```

#### 假人队形
你一定会发现 `/playerGroup` 创建出来的假人组所有假人都挤在一起, 这样的假人组根本不能完成更复杂的功能, 假人队形就是解决这个问题

在创建假人时使用 `formation` 来选择假人队形, 在队形后面填写行数 (仅支持多行的队形有行数), 最后可以填写方向 (默认视线方向)

使用 `interstice` 可以填写每个假人之间的间隔

#### formation 队形
```markdown
# 列
column
# 可叠加列 (可以一格内存在多个假人)
columnFold
# 排
row
# 可叠加排 (可以一格内存在多个假人)
rowFold
# 四边形
quadrangle
```

#### 例子
```markdown
# 创建一个组名为 bot 的假人组, 有 10 个假人, 并且排成一排
/playerGroup bot spawn 10 formation row
# 创建一个组名为 bot 的假人组, 有 10 个假人, 并且排成一列, 方向南
/playerGroup bot spawn 10 formation row north
# 创建一个组名为 bot 的假人组, 有 9 个假人, 并且排成四边形, 有三行
/playerGroup bot spawn 9 formation quadrangle 3
# 创建一个组名为 bot 的假人组, 有 9 个假人, 并且排成四边形, 有三行, 每一个假人间隔三格
/playerGroup bot spawn 9 formation quadrangle 3 interstice 3
# 创建一个组名为 bot 的假人组, 有 9 个假人, 并且排成一排, 格内存在 3 个假人, 每格假人间隔三格
/playerGroup bot spawn 9 formation rowFold 3 interstice 3
```

### /playerManage
使用此指令进行假人保存/管理/快捷操作, `/playerManage` 将使用 `splite` 保存假人数据

假人数据将保存在 mc 根目录下的 `pecaPlayer.db` 文件, 可以使用 sql/[sql 可视化工具](https://sqlitebrowser.org/) 查询

使用此指令将当前玩家的数据保存为假人数据 `/playerManage clone [用途]`

`/playerManage clone` 可以使用 `in` 修改保存数据的游戏模式, 使用 `to` 修改保存假人的名称

使用 `/playerManage [玩家名] save [用途]` 来保存指定假人/玩家的数据

使用 `/playerManage list` 列出所有保存假人

#### find 搜索
如果你保存了大量假人, 想找到一个假人, 或者你想看看一个范围有没有保存假人, 使用 `/playerManage list` 显然是不行的

这时候就可以使用 `/playerManage find` 来在数据库搜索假人, 可以从 4 种方面搜索假人

#### 例子
```markdown
# 搜索名称
/playerManage find [名称]
# 搜索游戏模式
/playerManage find gamemode [模式]
# 搜索维度
/playerManage find dimension [维度]
# 搜索坐标
/playerManage find pos [坐标]
```

可以一次从两个方面搜索假人

#### 例子
```markdown
# 搜索在主世界, 并且名称内带有 bot 的假人
/playerManage find dimension minecraft:overworld is bot
# 搜索名称内带有 test, 并且游戏模式为生存的假人
/playerManage find test in survival
# 搜索在当前玩家坐标不超过 50 的范围, 并且在地狱的假人
/playerManage find pos ~ ~ ~ inside 50 in minecraft:the_nether
# 搜索名称内带有 bot, 并且在当前玩家坐标不超过 20 的范围
/playerManage find bot at ~ ~ ~ inside 20
```

#### 搜索坐标范围
搜索坐标时使用 `inside` 指定搜索范围, 搜索范围为

`传入坐标(x, y, z) - inside <= 检查坐标(x, y, z) <= 传入坐标(x, y, z) + inside`

### /playerAuto
使用此指令进行假人任务, 使用 `/playerAuto [假人名] stop` 停止假人任务

#### 假人分类
使用 `/playerAuto [假人名] sort [物品]` 进行背包物品分类, 所有非传入的物品都将被假人扔岀

如果假人打开了任何容器都会使用传入物品填充容器

#### 假人合成
使用 `/playerAuto [假人名] craft [slot0] ... [slot8]` 进行假人合成, 假人合成必须假人已经打开工作台

所有成功合成物品都将被假人扔岀

#### 例子
```markdown
# 假人 bot_1 合成活塞
/playerAuto bot_1 craft minecraft:oak_planks minecraft:oak_planks minecraft:oak_planks minecraft:cobblestone minecraft:iron_ingot minecraft:cobblestone minecraft:cobblestone minecraft:redstone minecraft:cobblestone
# 假人 bot_1 拆解铁块
/playerAuto bot_1 craft minecraft:iron_block air air air air air air air air
# 假人 bot_1 合成铁块
/playerAuto bot_1 craft minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot minecraft:iron_ingot
```

#### 假人交易
使用 `/playerAuto [假人名] trading` 进行假人交易, 假人会使用背包中物品进行交易, 所有交易成功物品都将被假人扔岀, 
假人交易必须假人已经打开交易界面

可以使用 `from ... to ...` 指定交易范围, `from` 从哪里开始交易, `to`到哪里结束交易

## 特性

可以在游戏內使用 `/carpet` 后点击 `[PECA]` 查看/设置特性

![1.png](https://github.com/FengLiuFeseliud/Player-Extend-Carpet-Addition-Mod/blob/main/img/1.png?raw=true)

![2.png](https://github.com/FengLiuFeseliud/Player-Extend-Carpet-Addition-Mod/blob/main/img/2.png?raw=true)

![3.png](https://github.com/FengLiuFeseliud/Player-Extend-Carpet-Addition-Mod/blob/main/img/3.png?raw=true)