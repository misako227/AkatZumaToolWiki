[toc]


## 物品

### <font color=green>| </font> 飞剑
---


* 右键召唤后自动攻击玩家周围15格的怪物，每攻击10次会扣除玩家一点饱食度

![image](../img/fly.gif)



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


### <font color=green>| </font> 星弦弓（v0.0.2）
---

* 射出的箭带有粒子拖尾
* 可附魔：
  * 快速装填：每级减少20%蓄力时间
  * 自动射击：蓄力满自动射出
  * 星辰裁决：击中后有5%几率召唤法阵造成范围伤害
  * 其他：参考原版弓
* 配置文件可以修改很多参数

<img src="../img/bow1.JPG" width="400"> <img src="../img/bow2.JPG" width="400"> <img src="../img/bow3.jpg" width="400">

<br>

## <font color=green>| </font> 配置文件
---

* `config/Akatzumatool/akatzumatool.toml`
* 可以修改伤害，配置是否破坏地形，伤害白名单等
* 实体伤害白名单：
  * whitelist = ["touhou_little_maid:maid", "minecraft:cat"]
  * 默认添加了女仆的白名单，女仆不会受到伤害
  * 可使用CraftTweaker模组查看实体注册名。
  