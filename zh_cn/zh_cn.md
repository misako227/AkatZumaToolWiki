[toc]


## 物品

### <font color=green>| </font> 飞剑
---
* 左键发射剑气
* B键召唤/关闭 2把飞剑
* 右键召唤后自动攻击玩家周围15格的怪物，每攻击10次会扣除玩家一点饱食度

![image](../img/fly.gif)


### <font color=green>| </font> 真·飞剑（v0.0.3）
---
* 左键发射剑气
* 右键蓄力释放拔刀斩（扣除2点饱食度）
* V键释放次元斩（扣除2点饱食度）
* B键召唤/关闭 5把飞剑（扣除2点饱食度）
* C键蓄力释放咖喱棒

![image](../img/flysword/1.jpg)

![image](../img/flysword/2.jpg)
<img src="../img/flysword/4.jpg" width="400"> <img src="../img/flysword/3.jpg" width="400">

<br>


### <font color=green>| </font> 硬币
---

* 右键使用后扣除一个
* 车万女仆可以使用，放入背包后切换工作模式为电磁炮

![image](../img/coin1.png)


<br>


### <font color=green>| </font> 彩币
---
<!-- ![image](../img/coin.png)![image](../img/coin2.png) -->


* 右键使用后扣除耐久度
* 车万女仆可以使用
* 可破坏地形，配置文件中可以关闭，不会破坏FTB Chunks的领地
* 可附魔：
  * 快速装填：每级减少20%蓄力时间
  * 力量：每级增加25%伤害
  * 耐久：减少耐久度消耗
  * 经验修补：参考原版
  * 
<img src="../img/coin.JPG" width="400"> <img src="../img/coin2.JPG" width="400">


### <font color=green>| </font> 魔法弓（v0.0.2）
---

* 射出的箭带有粒子拖尾
* 攻击时有30%概率束缚敌人，使其无法移动
* 可附魔：
  * 快速装填：每级减少20%蓄力时间
  * 自动射击：蓄力满自动射出
  * 自动追踪：瞄准时可以锁定敌人，发射时往敌人位置发射
  * 星辰裁决：击中后有5%几率召唤法阵造成范围伤害
  * 其他：参考原版弓
* 配置文件可以修改很多参数

<img src="../img/bow1.JPG" width="400"> <img src="../img/bow2.JPG" width="400"> <img src="../img/bow3.jpg" width="400">

<br>

### <font color=green>| </font> 闪闪果实（v0.0.4）
---
* 食用后获得30秒的buff
* buff效果：
  * 免疫大部分伤害
  * 按alt键往前方瞬移
  * 长按ctrl键加速飞行
  * 怕水，在水中会清除buff


### <font color=green>| </font> 天雷战戟（v0.0.5）
---
* 投掷后召唤闪电
* 可附魔：
  * 引雷：投掷后造成持续伤害
  * 天雷：长按V键召唤范围闪电

<br>

## <font color=green>| </font> 配置文件
---

* `config/Akatzumatool/akatzumatool.toml`
* 可以修改伤害，配置是否破坏地形，伤害白名单等
* 实体伤害白名单：
  * whitelist = ["touhou_little_maid:maid", "minecraft:cat"]
  * 默认添加了女仆的白名单，女仆不会受到伤害
  * 可使用CraftTweaker模组查看实体注册名。
  