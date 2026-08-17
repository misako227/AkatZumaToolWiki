package com.z227.akatzumatool.linkMod.touhouLittleMaid;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.z227.akatzumatool.linkMod.touhouLittleMaid.task.MaidMeleeAttackTask;
import com.z227.akatzumatool.linkMod.touhouLittleMaid.task.MaidRangedAttackTask;

// Touhou Little Maid 女仆模组扩展入口，注册远程与飞剑近战任务。
@LittleMaidExtension
public class TouhouLittleMaid implements ILittleMaid {

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(new MaidRangedAttackTask());
        manager.add(new MaidMeleeAttackTask());
    }
}
